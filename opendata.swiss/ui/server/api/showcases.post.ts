import type {ShowcasesCollectionItem} from '@nuxt/content'
import type {H3Event, MultiPartData} from "h3";
import slugify from "slugify";
import * as yaml from 'yaml'
import showcaseSchema from '../../src/schema/showcase.js'
import {match, P} from "ts-pattern";
import git from "~~/server/lib/git";
import fs from "~~/server/lib/fs";

const languages = ['en', 'fr', 'de', 'it'] as const

type Language = typeof languages[number]
type Translated<FieldName extends string> = `${FieldName}[${Language}]`
type FormDataFieldNames = keyof ShowcasesCollectionItem | Translated<'title'> | Translated<'body'>
type ShowcaseTranslation = Omit<Partial<ShowcasesCollectionItem>, 'body'> & {
  body?: string
}
type Showcase = Record<Language, ShowcaseTranslation> & {
  slug?: string
}
type PayloadData = Array<Omit<MultiPartData, 'name'> & { name: FormDataFieldNames }>

const empty =  (): ShowcaseTranslation => ({
  active: true,
  categories: [],
  datasets: [],
  tags: []
})

interface ShowcaseStorage {
  prepare?(): Promise<boolean>
  writeFile(path: string, contents: string | Buffer): Promise<void>
  finalize(): Promise<boolean>
  rollback?(): Promise<void>
}

export default defineEventHandler(async (event) => {
  const t = await useTranslation(event)

  let storage: ShowcaseStorage | undefined

  const imageRoot = `img/uploads`

  const uploads: Array<() => Promise<void>> = []
  const body = await readMultipartFormData(event) as PayloadData
  const showcase: Showcase = {
    it: empty(),
    de: empty(),
    fr: empty(),
    en: empty()
  }
  const titleDe = body.find(field => field.name === 'title[de]')?.data?.toString()
  showcase.slug = slugify(titleDe!, {lower: true, locale: 'de'})

  if (process.env.GITHUB_OWNER) {
    const auth = process.env.GITHUB_APP_ID ? {
      appId: parseInt(process.env.GITHUB_APP_ID),
      privateKey: process.env.GITHUB_PRIVATE_KEY!
    } : process.env.GITHUB_TOKEN!

    storage = git(showcase.slug!, {
      auth,
      owner: process.env.GITHUB_OWNER,
      repo: process.env.GITHUB_REPO!,
      baseBranch: process.env.GITHUB_BASE_BRANCH!,
    })
    const branchCreated = await storage.prepare?.()
    if (!branchCreated) {
      event.node.res.statusCode = 409
      return {
        error: t('server.api.showcases.post.error.submission_exists')
      }
    }
  } else {
    const {public: {rootDir}} = useRuntimeConfig()
    storage = fs(rootDir)
  }

  for (const {name, data} of body) {
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
        toAll(showcase, 'body', data.toString())
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
        const imagePath = `${imageRoot}/${showcase.slug}-image.jpg`
        uploads.push(storage!.writeFile.bind(null, `public/${imagePath}`, data))
        toAll(showcase, 'image', imagePath)
      })
      .with(P.string.startsWith('datasets'), () => {
        const {id} = /^datasets\[(?<id>.+)]$/.exec(name)?.groups || {}
        if (id) {
          const label = data.toString()
          toAll(showcase, 'datasets', translation => {
            translation.datasets!.push({id, label})
          })
        }
      })
      .otherwise(() => {
        console.warn(`Unknown field: ${name}`)
      })
    }

  return validate(event, showcase) || (async () => {
    const success = await save(showcase, uploads, storage)

    if(success) {
      return {
        message: t('server.api.showcases.post.success')
      };
    }
    event.node.res.statusCode = 500
    return {
      error:  t('server.api.showcases.post.error.unspecified_error')
    };
  })()
});

async function save(showcase: Showcase, uploads: Array<() => Promise<void>>, storage: ShowcaseStorage) {
  const { slug } = showcase

  const writeContent = languages.map(language => {
    const path = `content/showcases/${slug}.${language}.md`

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

function toAll<K extends keyof ShowcaseTranslation>(showcase: Showcase, key: K, value: ShowcaseTranslation[K] | Setter) {
  for (const language of languages) {
    if(typeof value === 'function') {
     value(showcase[language])
    } else {
      showcase[language][key] = value
    }
  }
}

function validate(event: H3Event, showcase: Showcase) {
  for (const language of languages) {
    const { error } = showcaseSchema.safeParse(showcase[language])
    if (error) {
      event.node.res.statusCode = 400
      return error.issues
    }
  }
}
