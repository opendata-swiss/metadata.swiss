import {remark} from 'remark'
import strip from 'strip-markdown'

const frontMatterPattern = /^---[\s\S]*---/
const stemPattern = /^showcases\/(?<stem>.*)\.(?<lang>\w\w)$/

interface AggregateShowcase {
  id: string
  '@type': 'CreativeWork'
  title: Record<string, string | undefined>
  image: string | undefined
  description: Record<string, string | undefined>
  categories: string[]
  datasets: Array<{ id: string; label: string }>
  text: Record<string, string | undefined>
}

const ldContext = ['https://schema.org', {
  id: 'identifier',
  categories: { '@type': '@id' },
  datasets: {
    '@id': 'exampleOfWork',
  },
  title: {
    '@container': '@language',
  },
  description: {
    '@container': '@language',
  },
  text: {
    '@container': '@language',
  }
}];
export default defineEventHandler(async (event) => {
  const showcases = await queryCollection(event, 'showcases')
    .select('title', 'categories', 'datasets', 'description', 'rawbody', 'stem', 'image')
    .all()

  const aggregatedShowcases = showcases.reduce(async (promise, showcase) => {
    const arr = await promise

    const { stem, lang } = showcase.stem.match(stemPattern)?.groups!
    let aggregate = arr.find(({ id }) => id === stem)
    if (!aggregate) {
      aggregate = {
        id: stem,
        '@type': 'CreativeWork',
        title: {},
        image: showcase.image,
        description: {},
        categories: showcase.categories || [],
        datasets: showcase.datasets || [],
        text: {}
      }
      arr.push(aggregate)
    }

    aggregate.title[lang] = showcase.title || undefined
    aggregate.description[lang] = showcase.description || undefined
    aggregate.text[lang] =  await stripMarkdown(showcase.rawbody) || undefined

    return arr
  }, Promise.resolve<AggregateShowcase[]>([]))

  return {
    '@context': ldContext,
    '@graph': await aggregatedShowcases
  }
})

async function stripMarkdown(md: string) {
  const stripped = await remark().use(strip).process(md.replace(frontMatterPattern, '').trim())
  return stripped.value.toString()
}
