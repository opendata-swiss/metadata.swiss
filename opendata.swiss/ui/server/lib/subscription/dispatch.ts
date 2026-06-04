import type { AppLanguage } from '~/constants/langages'
import type { Subscriber } from '../../lib/listmonk'
import { generateToken } from '../../lib/listmonk/token'
import type Listmonk from '../../lib/listmonk'
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
  listmonk: Listmonk
  appUrl: string | URL
  key: string
  queryPageLimit?: number
  maxDatasetsPerEmail?: number
}

export async function dispatchDigest(digest: Digest, deps: DispatchDeps) {
  const { piveau, listmonk, appUrl, key, queryPageLimit, maxDatasetsPerEmail = 100 } = deps

  const minDate = digest === 'daily'
    ? new Date(Date.now() - 24 * 60 * 60 * 1000)
    : new Date(Date.now() - 7 * 24 * 60 * 60 * 1000)

  const datasetScroll = piveau.datasets.search({
    sort: 'modified+desc',
    limit: queryPageLimit,
    minDate,
    dateType: 'modified',
  }).scroll()

  const datasets: Pick<Dataset, 'id' | 'title'>[] = []
  for await (const page of datasetScroll) {
    datasets.push(...page.map(({ id, title, categories }) => ({ id, title, categories })))
  }

  const subscribers = await listmonk.subscribers.list()

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
        title: (dataset.title as Record<string, string>)[language] || dataset.id,
      })).slice(0, maxDatasetsPerEmail),
    }

    if (data.datasets.length) {
      batch.push((async () => {
        const res = await listmonk.transactional.sendDigest({
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

export function matchPreferences(subscriber: Subscriber) {
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
