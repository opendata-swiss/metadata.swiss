import { remark } from 'remark'
import strip from 'strip-markdown'
import remarkFrontmatter from 'remark-frontmatter'
import { dcat, dcterms, rdfs, schema } from '@tpluscode/rdf-ns-builders'
import type { ShowcasesCollectionItem } from '@nuxt/content'
import { execSync } from 'node:child_process'
import { join } from 'node:path'

const stemPattern = /showcases\/(?<stem>.*)\.(?<lang>\w\w)$/

interface AggregateShowcase {
  'id': string
  'identifier': string
  '@type': string[]
  'type': string
  'title': Record<string, string | undefined>
  'image': string | undefined
  'abstract': Record<string, string | undefined>
  'categories': string[]
  'datasets': Array<{ identifier: string, label: string }>
  'text': Record<string, string | undefined>
  'tag': string[]
  'modified': string | undefined
  'issued': string | undefined
}

const ldContext = {
  id: '@id',
  label: rdfs.label.value,
  type: {
    '@id': dcterms.type.value,
    '@type': '@id',
  },
  categories: {
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
  identifier: dcterms.identifier.value,
  image: schema.image.value,
  tag: dcat.keyword.value,
  modified: dcterms.modified.value,
  issued: dcterms.issued.value,
  Dataset: dcat.Dataset.value,
  piveau: 'https://piveau.eu/ns/voc#',
}
export default defineEventHandler(async (event) => {
  const { public: { rootDir } } = useRuntimeConfig(event)
  const showcases = await queryCollection(event, 'showcases')
    .select('title', 'categories', 'datasets', 'description', 'rawbody', 'stem', 'image', 'tags', 'type')
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
        'image': showcase.image,
        'abstract': {},
        'categories': showcase.categories || [],
        'datasets': mapDatasets(showcase.datasets) || [],
        'text': {},
        'tag': showcase.tags || [],
        modified,
        issued,
      }
      arr.push(aggregate)
    }

    aggregate.title[lang] = showcase.title || undefined
    aggregate.abstract[lang] = showcase.description || undefined
    aggregate.text[lang] = await stripMarkdown(showcase.rawbody) || undefined

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
