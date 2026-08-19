import { remark } from 'remark'
import strip from 'strip-markdown'
import remarkFrontmatter from 'remark-frontmatter'
import { dcat, dcterms, schema, xsd } from '@tpluscode/rdf-ns-builders'
import type { ShowcasesCollectionItem } from '@nuxt/content'
import { execSync } from 'node:child_process'
import { join } from 'node:path'
import { queryCollection } from '@nuxt/content/server'

const stemPattern = /showcases\/(?<stem>.*)\.(?<lang>\w\w)$/

interface AggregateShowcase {
  'id': string
  'identifier': string
  '@type': string[]
  'type': string
  'title': Record<string, string | undefined>
  'images': string[]
  'abstract': Record<string, string | undefined>
  'themes': string[]
  'datasets': Array<{ identifier: string, label: string }>
  'text': Record<string, string | undefined>
  'keywords': string[]
  'modified': string | undefined
  'issued': string | undefined
  'pinned': boolean
}

type TypeCoercion = { '@id': string, '@type': string }
type Container = { '@id': string, '@container': string }
type Mappings = { Dataset: string, piveau: string }
type Context = Record<keyof Omit<AggregateShowcase, '@type'>, string | TypeCoercion | Container> & Mappings

const ldContext: Context = {
  id: '@id',
  type: {
    '@id': dcterms.type.value,
    '@type': '@id',
  },
  themes: {
    '@id': dcat.theme.value,
    '@type': '@id',
  },
  datasets: {
    '@id': dcterms.references.value,
    '@type': '@id',
  },
  title: {
    '@id': dcterms.title.value,
    '@container': '@language',
  },
  abstract: {
    '@id': dcterms.abstract.value,
    '@container': '@language',
  },
  text: {
    '@id': schema.text.value,
    '@container': '@language',
  },
  pinned: {
    '@id': 'piveau:pinned',
    '@type': xsd.boolean.value,
  },
  identifier: dcterms.identifier.value,
  images: schema.image.value,
  keywords: dcat.keyword.value,
  modified: dcterms.modified.value,
  issued: dcterms.issued.value,
  Dataset: dcat.Dataset.value,
  piveau: 'https://piveau.eu/ns/voc#',
}

export default defineEventHandler(async (event) => {
  const { public: { rootDir } } = useRuntimeConfig(event)
  const showcases = await queryCollection(event, 'showcases')
    .select('title', 'themes', 'datasets', 'description', 'rawbody', 'stem', 'images', 'keywords', 'type', 'pinned')
    .where('active', '=', true)
    .all()

  const aggregatedShowcases = showcases.reduce(async (promise, showcase) => {
    const arr = await promise

    const { stem, lang } = showcase.stem.match(stemPattern)?.groups || {}
    if (!stem || !lang) {
      console.warn(`${showcase.stem} did not match stem pattern`)
      return arr
    }

    const id = `showcase/${stem}`
    let aggregate = arr.find(agg => agg.id === id)
    if (!aggregate) {
      const { modified, issued } = getShowcaseDates(rootDir, stem)
      aggregate = {
        id,
        'identifier': stem,
        '@type': ['Showcase', 'Dataset', 'piveau:CustomResource'],
        'type': showcase.type,
        'title': {},
        'images': showcase.images || [],
        'abstract': {},
        'themes': showcase.themes || [],
        'datasets': mapDatasets(showcase.datasets) || [],
        'text': {},
        'keywords': showcase.keywords || [],
        'pinned': showcase.pinned || false,
        modified,
        issued,
      }
      arr.push(aggregate)
    }

    aggregate.title[lang] = showcase.title || ''
    aggregate.abstract[lang] = showcase.description || ''
    aggregate.text[lang] = await stripMarkdown(showcase.rawbody) || ''

    return arr
  }, Promise.resolve<AggregateShowcase[]>([]))

  return event.respondWith(new Response(JSON.stringify({
    '@context': [
      { '@base': 'http://localhost:3000/' },
      ldContext,
    ],
    '@graph': await aggregatedShowcases,
  }), {
    headers: { 'Content-Type': 'application/ld+json' },
  }))
})

function mapDatasets(datasets: ShowcasesCollectionItem['datasets'] | undefined) {
  return datasets?.map(ds => ({
    identifier: ds.id,
    label: ds.label,
  })) || []
}

async function stripMarkdown(md: string | undefined) {
  if (!md) {
    return ''
  }

  const stripped = await remark()
    .use(strip)
    .use(remarkFrontmatter)
    .process(md)
  return stripped.value.toString()
}

function getShowcaseDates(rootDir: string, stem: string) {
  try {
    const showcasesDir = join(rootDir, 'content/showcases')
    const files = [
      `${stem}.de.md`,
      `${stem}.en.md`,
      `${stem}.fr.md`,
      `${stem}.it.md`,
    ].join(' ')

    // Get all commit dates for the showcase files, sorted from newest to oldest
    // Note: Accurate dates require a repository clone with sufficient history (avoid --depth 1)
    const command = `git log --format=%aI -- ${files}`
    const result = execSync(command, { cwd: showcasesDir, encoding: 'utf-8' })

    const dates = result.trim().split('\n').filter(Boolean)

    return {
      modified: dates[0],
      issued: dates.at(-1),
    }
  }
  catch (e) {
    console.error(`Failed to get git dates for ${stem}`, e)
    return {
      modified: undefined,
      issued: undefined,
    }
  }
}
