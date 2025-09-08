import { ShowcasesCollectionItem } from '@nuxt/content'
import type {MultiPartData} from "h3";
import slugify from "slugify";
import * as fs from 'node:fs/promises'
import * as yaml from 'yaml'

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
  const { public: { contentRoot } } = useRuntimeConfig();

  const body = await readMultipartFormData(event) as PayloadData
  const showcase: Showcase = {
    it: empty(),
    de: empty(),
    fr: empty(),
    en: empty()
  }

  for (const { name, data } of body) {
    switch (name) {
      case 'title-de':
        showcase.slug = slugify(data.toString(), { lower: true, locale: 'de' })
      case 'title-fr':
      case 'title-en':
      case 'title-it':
        const language = /^title-(?<lang>\w\w)$/.exec(name)?.groups?.lang as Language
        showcase[language].title = data.toString()
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
    }
  }

  // TODO: choose to save to GitHub or locally based on environment
  await save(showcase, contentRoot)

  return { message: 'Showcase submission received successfully.' };
});

function save(showcase: Showcase, contentRoot: string) {
  const { slug } = showcase

  return Promise.all(languages.map(language => {
    const path = `${contentRoot}/showcases/${slug}.${language}.md`

    const { body, ...meta } = showcase[language]
    const frontMatter = yaml.stringify(meta)

    return fs.writeFile(path, `---\n${frontMatter}---\n${body}`)
  }))
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
