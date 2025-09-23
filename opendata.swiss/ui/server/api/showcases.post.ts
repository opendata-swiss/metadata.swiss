import type {ShowcasesCollectionItem} from '@nuxt/content'
import type {H3Event, MultiPartData} from "h3";
import slugify from "slugify";
import * as fs from 'node:fs/promises'
import * as yaml from 'yaml'
import showcaseSchema from '../../src/schema/showcase.js'

const languages = ['en', 'fr', 'de', 'it'] as const

type Language = typeof languages[number]
type Translated<FieldName extends string> = `${FieldName}-${Language}`
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

export default defineEventHandler(async (event) => {
  let rootDir: string

  if (process.env.NODE_ENV === 'production') {
    // TODO: clone repo, get path to content
    // rootDir = checkoutPath
    throw new Error('Saving to GitHub not implemented yet.')
  } else {
    ({ public: { rootDir } } = useRuntimeConfig())
  }

  const contentRoot = `${rootDir}/content`
  const imageRoot = `${rootDir}/public/img/uploads`

  const uploads: Array<() => Promise<void>> = []
  const body = await readMultipartFormData(event) as PayloadData
  const showcase: Showcase = {
    it: empty(),
    de: empty(),
    fr: empty(),
    en: empty()
  }
  const titleDe = body.find(field => field.name === 'title-de')?.data?.toString()
  showcase.slug = slugify(titleDe!, { lower: true, locale: 'de' })

  for (const { name, data } of body) {
    switch (name) {
      case 'title-de':
      case 'title-fr':
      case 'title-en':
      case 'title-it': {
        const language = /^title-(?<lang>\w\w)$/.exec(name)?.groups?.lang as Language
        showcase[language].title = data.toString()
      }
        break
      case 'url':
      case "type":
        toAll(showcase, name, data.toString())
        break
      case "body-de":
      case "body-en":
      case "body-it":
      case "body-fr":
        toAll(showcase, 'body', data.toString())
        break
      case "tags":
        toAll(showcase, 'tags', data.toString().split(',').map(tag => tag.trim()))
        break
      case "categories":
        toAll(showcase, 'categories', (translation) => {
          translation.categories!.push(data.toString())
        })
        break
      case "image": {
        const imagePath = `${imageRoot}/${showcase.slug}-image.jpg`
        uploads.push(fs.writeFile.bind(null, imagePath, data))
      }
        break
      default:
        console.warn(`Unknown field: ${name}`)
        break
    }
  }

  return validate(event, showcase) || (async () => {
    // TODO: choose to save to GitHub or locally based on environment
    await save(showcase, uploads, contentRoot)

    if (process.env.NODE_ENV === 'production') {
      // TODO: commit and push to repo
      throw new Error('Saving to GitHub not implemented yet.')
    }

    return { message: 'Showcase submission received successfully.' };
  })()
});

function save(showcase: Showcase, uploads: Array<() => Promise<void>>, contentRoot: string) {
  const { slug } = showcase

  const writeContent = languages.map(language => {
    const path = `${contentRoot}/showcases/${slug}.${language}.md`

    const { body, ...meta } = showcase[language]
    const frontMatter = yaml.stringify(meta)

    return fs.writeFile(path, `---\n${frontMatter}---\n${body}`)
  })

  return Promise.all([...writeContent, ...uploads.map(upload => upload())])
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
