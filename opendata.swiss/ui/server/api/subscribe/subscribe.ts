import type { H3Event } from 'h3'
import type * as Linkmonk from './listmonk.types'
import { getLanguage } from '#server/lib/locale'

export function subscribe(key: 'categories' | 'datasets' | 'organisations', queryParam: 'category' | 'organisation' | 'dataset') {
  return async (event: H3Event) => {
    const { user: { email } } = event.context
    const query = getQuery(event)
    const values = Array.isArray(query[queryParam]) ? query[queryParam] : [query[queryParam]]
    const language = getLanguage(event)

    const { listmonk: { api: listmonk } } = useRuntimeConfig()

    const authorization = {
      Authorization: `token ${listmonk.user}:${listmonk.token}`,
    }

    const getSubscribers = await fetch(`${listmonk.url}api/subscribers?query=subscribers.email = '${email}'`, {
      headers: authorization,
    })

    if (!getSubscribers.ok) {
      throw new Error(`Failed to fetch subscribers: ${getSubscribers.status} ${getSubscribers.statusText}`)
    }

    const subscribers: Linkmonk.Envelope<Linkmonk.Subscribers> = await getSubscribers.json()

    const subscriber = subscribers.data.results.pop() || {
      name: '',
      status: 'enabled',
      lists: [],
      attribs: {
        language,
        [key]: values,
      },
    }

    if ('id' in subscriber) {
      await fetch(`${listmonk.url}api/subscribers/${subscriber.id}`, {
        method: 'PATCH',
        body: JSON.stringify({
          attribs: {
            [key]: [...new Set([
              ...(subscriber.attribs?.[key] || []),
              ...values,
            ])],
          },
        }),
        headers: {
          ...authorization,
          'content-type': 'application/json',
        },
      })
    }
    else {
      await fetch(`${listmonk.url}api/subscribers`, {
        method: 'POST',
        body: JSON.stringify(subscriber),
        headers: {
          ...authorization,
          'content-type': 'application/json',
        },
      })
    }

    return {
      message: 'Subscription successful',
    }
  }
}
