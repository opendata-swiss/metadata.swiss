import {remark} from 'remark'
import strip from 'strip-markdown'
import remarkFrontmatter from "remark-frontmatter";
import {dcat, dcterms, rdfs, schema} from "@tpluscode/rdf-ns-builders";

const stemPattern = /^showcases\/(?<stem>.*)\.(?<lang>\w\w)$/

interface AggregateShowcase {
  id: string
  '@type': 'Showcase'
  title: Record<string, string | undefined>
  image: string | undefined
  abstract: Record<string, string | undefined>
  categories: string[]
  datasets: Array<{ id: string; label: string }>
  text: Record<string, string | undefined>
  tag: string[]
}

const ldContext = {
  '@base': 'https://example.org/',
  id: '@id',
  label: rdfs.label.value,
  categories: {
    '@id': dcat.theme.value,
    '@type': '@id'
  },
  datasets: {
    '@id': dcterms.references.value,
    '@type': '@id'
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
  image: schema.image.value,
  tag: dcat.keyword.value,
};
export default defineEventHandler(async (event) => {
  const showcases = await queryCollection(event, 'showcases')
    .select('title', 'categories', 'datasets', 'description', 'rawbody', 'stem', 'image')
    .all()

  const aggregatedShowcases = showcases.reduce(async (promise, showcase) => {
    const arr = await promise

    const { stem, lang } = showcase.stem.match(stemPattern)?.groups || {}
    let aggregate = arr.find(({ id }) => id === stem)
    if (!aggregate) {
      aggregate = {
        id: stem,
        '@type': 'Showcase',
        title: {},
        image: showcase.image,
        abstract: {},
        categories: showcase.categories || [],
        datasets: showcase.datasets || [],
        text: {},
        tag: [],
      }
      arr.push(aggregate)
    }

    aggregate.title[lang] = showcase.title || undefined
    aggregate.abstract[lang] = showcase.description || undefined
    aggregate.text[lang] =  await stripMarkdown(showcase.rawbody) || undefined

    return arr
  }, Promise.resolve<AggregateShowcase[]>([]))

  return {
    '@context': ldContext,
    '@graph': await aggregatedShowcases
  }
})

async function stripMarkdown(md: string) {
  const stripped = await remark()
    .use(strip)
    .use(remarkFrontmatter)
    .process(md)
  return stripped.value.toString()
}
