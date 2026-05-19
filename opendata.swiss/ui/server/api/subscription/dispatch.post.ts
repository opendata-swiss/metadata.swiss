import type { AppLanguage } from '~/constants/langages'
import type { Subscriber } from '#server/lib/listmonk/index.js'
import listmonk from '#server/lib/listmonk/index.js'
import { generateToken } from '#server/lib/listmonk/token'

interface Category {
  id: string
}

interface Dataset {
  id: string
  categories?: Category[]
  title: Record<AppLanguage, string>
}

interface SearchResult {
  result: {
    results: Dataset[]
  }
}

interface TemplateData {
  unsubscribeLink: string
  datasetPageBaseUrl: string
  datasets: Array<{
    id: string
    title: string
  }>
}

export default defineEventHandler(async (event) => {
  const {
    public: {
      piveauHubSearchUrl: baseUrl,
    },
    listmonk: listmonkConfig,
  } = useRuntimeConfig()
  const body = await readFormData(event)
  const digest = body.get('digest')?.toString()

  if (!digest || (digest !== 'daily' && digest !== 'weekly')) {
    throw createError({ statusCode: 400, statusMessage: 'Missing or invalid digest parameter. Must be "daily" or "weekly"' })
  }

  const minDate = digest === 'daily'
    ? new Date(Date.now() - 24 * 60 * 60 * 1000).toISOString()
    : new Date(Date.now() - 7 * 24 * 60 * 60 * 1000).toISOString()

  const searchUrl = new URL('search', baseUrl as string)
  searchUrl.searchParams.set('sort', 'modified+desc')
  searchUrl.searchParams.set('limit', '100')
  searchUrl.searchParams.set('filters', 'dataset')
  searchUrl.searchParams.set('minDate', minDate)
  searchUrl.searchParams.set('dateType', 'modified')
  const searchRes = await fetch(searchUrl)

  if (!searchRes.ok) {
    return createError({
      statusCode: searchRes.status,
      message: 'Failed to fetch latest datasets',
      cause: await searchRes.text(),
    })
  }

  const { result: { results: datasets } }: SearchResult = await searchRes.json()

  const Listmonk = listmonk(listmonkConfig)
  const subscribers = await Listmonk.subscribers.list()

  let emailsSent = 0
  let emailsFailed = 0
  let batch: Promise<void>[] = []
  for (const subscriber of subscribers) {
    const language = subscriber.attribs?.language || 'de'
    const unsubscribeLink = new URL(`/${language}/subscription/preferences`, listmonkConfig.template.datasetPageUrl)
    unsubscribeLink.searchParams.set('id', subscriber.id.toString())
    unsubscribeLink.searchParams.set('token', generateToken(subscriber.id.toString()))

    const data: TemplateData = {
      unsubscribeLink: unsubscribeLink.toString(),
      datasetPageBaseUrl: listmonkConfig.template.datasetPageUrl,
      datasets: datasets.filter(matchPreferences(subscriber)).map(dataset => ({
        id: dataset.id,
        title: dataset.title[language] || dataset.id,
      })),
    }

    if (data.datasets.length) {
      batch.push((async () => {
        const res = await Listmonk.transactional.send({
          subscriber: subscriber.id,
          language,
          data,
        })

        if (!res.ok) {
          console.error(`Failed to send email to subscriber ${subscriber.id}: ${await res.text()}`)
          emailsFailed++
        }
        else {
          emailsSent++
        }
      })())
    }

    if (batch.length === 50) {
      await Promise.all(batch)
      batch = []
    }
  }

  await Promise.all(batch)

  return {
    message: `Subscriber emails: sent=${emailsSent} failed=${emailsFailed}`,
  }
})

function matchPreferences(subscriber: Subscriber) {
  return (dataset: Dataset) => {
    if (subscriber.attribs?.datasets?.length) {
      return subscriber.attribs.datasets.includes(dataset.id)
    }

    if (subscriber.attribs?.categories?.length) {
      return dataset.categories?.some(category => subscriber.attribs?.categories?.includes(category.id))
    }

    return false
  }
}
