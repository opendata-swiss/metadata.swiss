import type { AppLanguage } from '~/constants/langages'
import type { Subscriber } from '../../lib/listmonk'
import { generateToken } from '../../lib/listmonk/token'
import type { Dataset, HubSearch } from '../../lib/piveau'

export interface TemplateData {
  unsubscribeLink: string
  datasetPageBaseUrl: string
  datasets: Array<{
    id: string
    title: string
  }>
}

export type Digest = 'daily' | 'weekly'

export interface DispatchDeps {
  piveau: HubSearch
  listSubscribers: () => Promise<Subscriber[]>
  sendDigest: ({ subscriber, language, data }: { subscriber: number, language: AppLanguage, data: TemplateData }) => Promise<{ ok: boolean, text: () => Promise<string> }>
  appUrl: string | URL
  key: string
  queryPageLimit?: number
  maxDatasetsPerEmail?: number
}

export async function dispatchDigestForDatasets(
  datasets: Pick<Dataset, 'id' | 'title' | 'categories'>[],
  subscribers: Pick<Subscriber, 'id' | 'attribs'>[],
  deps: Pick<DispatchDeps, 'sendDigest' | 'appUrl' | 'key' | 'maxDatasetsPerEmail'>) {
  const { sendDigest, appUrl, key, maxDatasetsPerEmail } = deps

  let emailsSent = 0
  let emailsFailed = 0
  let batch: Promise<void>[] = []
  for (const subscriber of subscribers) {
    const language: AppLanguage = (subscriber.attribs?.language as AppLanguage) || 'de'
    const unsubscribeLink = new URL(`/${language}/subscription/preferences`, appUrl)
    unsubscribeLink.searchParams.set('id', subscriber.id.toString())
    unsubscribeLink.searchParams.set('token', generateToken(subscriber.id.toString(), key))

    const data: TemplateData = {
      unsubscribeLink: unsubscribeLink.toString(),
      datasetPageBaseUrl: new URL(`/${language}/datasets/`, appUrl).toString(),
      datasets: datasets.filter(matchPreferences(subscriber)).map(dataset => ({
        id: dataset.id,
        title: dataset.title[language] || dataset.id,
      })).slice(0, maxDatasetsPerEmail),
    }

    if (data.datasets.length) {
      batch.push((async () => {
        const res = await sendDigest({
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

  return { emailsSent, emailsFailed }
}

export async function dispatchDigest(digest: Digest, deps: DispatchDeps) {
  const { piveau, listSubscribers, sendDigest, appUrl, key, queryPageLimit, maxDatasetsPerEmail } = deps

  const minDate = digest === 'daily'
    ? new Date(Date.now() - 24 * 60 * 60 * 1000)
    : new Date(Date.now() - 7 * 24 * 60 * 60 * 1000)

  const datasetScroll = piveau.datasets.search({
    sort: 'modified+desc',
    limit: queryPageLimit,
    minDate,
    dateType: 'modified',
  }).scroll()

  const datasets: Pick<Dataset, 'id' | 'title' | 'categories'>[] = []
  for await (const page of datasetScroll) {
    datasets.push(...page.map(({ id, title, categories }) => ({ id, title, categories })))
  }

  const subscribers = await listSubscribers()
  return dispatchDigestForDatasets(datasets, subscribers, { sendDigest, appUrl, key, maxDatasetsPerEmail })
}

export function matchPreferences(subscriber: Pick<Subscriber, 'id' | 'attribs'>) {
  return (dataset: Dataset) => {
    let doesMatch: boolean | undefined = false

    if (subscriber.attribs?.datasets?.length) {
      doesMatch = subscriber.attribs.datasets.includes(dataset.id)
    }

    if (!doesMatch && subscriber.attribs?.categories?.length) {
      doesMatch = dataset.categories?.some(category => subscriber.attribs?.categories?.includes(category.id))
    }

    return doesMatch
  }
}
