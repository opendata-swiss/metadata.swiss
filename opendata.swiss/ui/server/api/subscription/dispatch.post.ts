import type { Subscriber } from '#server/lib/listmonk/index.js'
import listmonk from '#server/lib/listmonk/index.js'
import { generateToken } from '#server/lib/listmonk/token'
import type { Dataset } from '#server/lib/piveau'
import { HubSearch } from '#server/lib/piveau'

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
    appUrl,
  } = useRuntimeConfig()
  const body = await readFormData(event)
  const digest = body.get('digest')?.toString()

  if (!digest || (digest !== 'daily' && digest !== 'weekly')) {
    throw createError({ statusCode: 400, statusMessage: 'Missing or invalid digest parameter. Must be "daily" or "weekly"' })
  }

  const minDate = digest === 'daily'
    ? new Date(Date.now() - 24 * 60 * 60 * 1000)
    : new Date(Date.now() - 7 * 24 * 60 * 60 * 1000)

  const piveau = new HubSearch(baseUrl)
  const datasets = await piveau.datasets.search({
    sort: 'modified+desc',
    limit: 100,
    minDate,
    dateType: 'modified',
  })

  if (datasets instanceof Error) {
    return createError({
      statusCode: 500,
      message: 'Failed to fetch latest datasets',
      cause: datasets.cause,
    })
  }

  const Listmonk = new listmonk(listmonkConfig)
  const subscribers = await Listmonk.subscribers.list()

  let emailsSent = 0
  let emailsFailed = 0
  let batch: Promise<void>[] = []
  for (const subscriber of subscribers) {
    const language = subscriber.attribs?.language || 'de'
    const unsubscribeLink = new URL(`/${language}/subscription/preferences`, appUrl)
    unsubscribeLink.searchParams.set('id', subscriber.id.toString())
    unsubscribeLink.searchParams.set('token', generateToken(subscriber.id.toString()))

    const data: TemplateData = {
      unsubscribeLink: unsubscribeLink.toString(),
      datasetPageBaseUrl: new URL(`/${language}/datasets/`, appUrl).toString(),
      datasets: datasets.filter(matchPreferences(subscriber)).map(dataset => ({
        id: dataset.id,
        title: dataset.title[language] || dataset.id,
      })),
    }

    if (data.datasets.length) {
      batch.push((async () => {
        const res = await Listmonk.transactional.sendDigest({
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
