import {remark} from 'remark'
import strip from 'strip-markdown'

const frontMatterPattern = /^---[\s\S]*---/

export default defineEventHandler(async (event) => {
  const showcases = await queryCollection(event, 'showcases')
    .select('categories', 'datasets', 'description', 'rawbody')
    .all()

  const showcasesLD = showcases.map(async (showcase) => ({
    ...showcase,
    '@type': 'CreativeWork',
    rawbody: await stripMarkdown(showcase.rawbody),
  }))

  return {
    '@context': ['https://schema.org', {
      id: 'identifier',
      rawbody: 'text',
      categories: { '@type': '@id' },
      datasets: {
        '@id': 'hasPart',
      }
    }],
    '@graph': await Promise.all(showcasesLD)
  }
})

function stripMarkdown(md: string) {
  return remark().use(strip).process(md.replace(frontMatterPattern, '').trim())
}
