import type { ShowcasesCollectionItem } from '@nuxt/content'
import type { H3Event, MultiPartData } from 'h3'
import slugify from 'slugify'
import * as yaml from 'yaml'
import { unified } from 'unified'
import remarkParse from 'remark-parse'
import { visit } from 'unist-util-visit'
import { submissionSchema } from '~~/src/schema/showcase'
import remarkStringify from 'remark-stringify'
import { match, P } from 'ts-pattern'
import git from '~~/server/lib/git'
import fs from '~~/server/lib/fs'
import type { AppLanguage as Language } from '~/constants/langages'
import { APP_LANGUAGES as languages } from '~/constants/langages'

type Translated<FieldName extends string> = `${FieldName}[${Language}]`
type FormDataFieldNames = keyof ShowcasesCollectionItem | Translated<'title'> | Translated<'body'>
type ShowcaseTranslation = Omit<Partial<ShowcasesCollectionItem>, 'body'> & {
  body?: string
}
type Showcase = Record<Language, ShowcaseTranslation> & {
  slug: string
}
type PayloadData = Array<Omit<MultiPartData, 'name'> & { name: FormDataFieldNames }>

const empty = (): ShowcaseTranslation => ({
  active: true,
  categories: [],
  datasets: [],
  tags: [],
})

interface ShowcaseStorage {
  prepare?(): Promise<boolean>
  writeFile(path: string, contents: string | Buffer): Promise<void>
  finalize(): Promise<boolean>
  rollback?(): Promise<void>
}

export default defineEventHandler(async (event) => {
  const logger = console

  const t = await useTranslation(event)

  let storage: ShowcaseStorage

  const uploads: Array<() => Promise<void>> = []
  const reqBody = await readMultipartFormData(event) as PayloadData
  const showcase: Showcase = {
    slug: '',
    it: empty(),
    de: empty(),
    fr: empty(),
    en: empty(),
  }

  const slug = createSlug(reqBody)!

  if (!slug) {
    event.node.res.statusCode = 400
    return {
      error: t('server.api.showcases.post.error.missing_content'),
    }
  }
  showcase.slug = slug

  if (process.env.GITHUB_TOKEN || process.env.GITHUB_APP_ID) {
    storage = git(showcase.slug)
    const branchCreated = await storage.prepare?.()
    if (!branchCreated) {
      event.node.res.statusCode = 409
      return {
        error: t('server.api.showcases.post.error.submission_exists'),
      }
    }
    logger.info('Initialized git storage backend')
  }
  else {
    const { public: { rootDir } } = useRuntimeConfig()
    storage = fs(rootDir)
    logger.info('Initialized filesystem storage backend')
  }

  const processedBodies: Array<Promise<void>> = []
  const allImagePaths = new Map<string, string>()

  for (const { name, data } of reqBody) {
    match(name)
      .with(P.string.startsWith('title'), () => {
        const language = /^title\[(?<lang>\w\w)]$/.exec(name)?.groups?.lang as Language
        showcase[language].title = data.toString()
      })
      .with(P.union('url', 'type'), (urlOrType) => {
        const value = data.toString()
        if (value) {
          toAll(showcase, urlOrType, value)
        }
      })
      .with(P.string.startsWith('body'), () => {
        processedBodies.push((async () => {
          const language = /^body\[(?<lang>\w\w)]$/.exec(name)?.groups?.lang as Language
          const rawBody = data.toString()
          const {
            body,
            images,
          } = await extractDataImages(rawBody, `assets/${showcase.slug}-image-`, allImagePaths)
          showcase[language].body = body
          for (const image of images) {
            uploads.push(storage.writeFile.bind(null, image.path, image.data))
          }
        })())
      })
      .with('tags', () => {
        const tags = data.toString().split(',').map(tag => tag.trim()).filter(tag => tag.length > 0)
        if (tags.length > 0) {
          toAll(showcase, 'tags', tags)
        }
      })
      .with('categories', () => {
        toAll(showcase, 'categories', (translation) => {
          const category = data.toString().trim()
          if (category) {
            translation.categories!.push(category)
          }
        })
      })
      .with('image', () => {
        const imageFileName = `${showcase.slug}-image.jpg`
        uploads.push(storage!.writeFile.bind(null, `assets/${imageFileName}`, data))
        toAll(showcase, 'image', `/cms/assets/${imageFileName}`)
      })
      .with(P.string.startsWith('datasets'), () => {
        const { id } = /^datasets\[(?<id>.+)]$/.exec(name)?.groups || {}
        if (id) {
          const label = data.toString()
          toAll(showcase, 'datasets', (translation) => {
            translation.datasets!.push({ id, label })
          })
        }
      })
      .otherwise(() => {
        console.warn(`Unknown field: ${name}`)
      })
  }

  await Promise.all(processedBodies)

  const errors = validate(event, showcase, t)

  if (errors) {
    logger.info('Validation failed. Reverting showcase submission.')
    await storage.rollback?.()
    return errors
  }

  if (!await save(showcase, uploads, storage)) {
    await storage.rollback?.()
    event.node.res.statusCode = 500
    return {
      error: t('server.api.showcases.post.error.unspecified_error'),
    }
  }

  return {
    message: t('server.api.showcases.post.success'),
  }
})

async function save(showcase: Showcase, uploads: Array<() => Promise<void>>, storage: ShowcaseStorage) {
  const { slug } = showcase

  const writeContent = languages.map((language) => {
    const path = `showcases/${slug}.${language}.md`

    const { body, ...meta } = showcase[language]
    const frontMatter = yaml.stringify(meta)

    return storage.writeFile(path, `---\n${frontMatter}---\n${body}`)
  })

  await Promise.all([...writeContent, ...uploads.map(upload => upload())])
  return storage.finalize?.()
}

interface Setter {
  (showcase: ShowcaseTranslation): void
}

async function extractDataImages(rawBody: string, imagePathPrefix: string, previousImages: Map<string, string>) {
  const processor = unified()
    .use(remarkParse)
    .use(remarkStringify)

  const tree = processor.parse(rawBody)
  const images = Array<{ path: string, data: Buffer }>()

  visit(tree, 'image', (node) => {
    if (node.url.startsWith('data:')) {
      const match = /^data:(image\/(?<ext>\w+));base64,(?<data>.+)$/.exec(node.url)
      if (match?.groups) {
        const { ext, data } = match.groups
        if (previousImages.has(data)) {
          node.url = previousImages.get(data)!
        }
        else {
          const path = `${imagePathPrefix}${previousImages.size}.${ext}`
          images.push({
            path,
            data: Buffer.from(match.groups.data, 'base64'),
          })
          previousImages.set(data, path)
          node.url = path
        }
      }
    }
  })

  return {
    body: processor.stringify(tree),
    images,
  }
}

function toAll<K extends keyof ShowcaseTranslation>(showcase: Showcase, key: K, value: ShowcaseTranslation[K] | Setter) {
  for (const language of languages) {
    if (typeof value === 'function') {
      value(showcase[language])
    }
    else {
      showcase[language][key] = value
    }
  }
}

function createSlug(showcase: PayloadData) {
  for (const locale of languages) {
    const titleField = `title[${locale}]`
    const title = showcase.find(field => field.name === titleField)?.data.toString()
    if (title) {
      return slugify(title, { lower: true, locale })
    }
  }

  return undefined
}

function validate(event: H3Event, showcase: Showcase, t: (key: string) => string) {
  const { error } = submissionSchema(t).safeParse(showcase)
  if (error) {
    event.node.res.statusCode = 400
    return error.issues
  }
}
