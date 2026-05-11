import type { H3Event } from 'h3'
import type * as Linkmonk from './listmonk.types'
import { getLanguage } from '#server/lib/locale'

type PayloadItem = {
  name: 'category' | 'organisation' | 'dataset'
  data: Buffer
}

export function subscribe(key: 'categories' | 'datasets' | 'organisations', fieldName: PayloadItem['name']) {
  return async (event: H3Event) => {
    const { user: { name, email } } = event.context
    const payload = await readFormData(event)
    const values = payload.getAll(fieldName).map(field => field.toString()).filter(Boolean)
    const language = getLanguage(event)

    if (!values.length) {
      return createError({
        statusCode: 400,
        statusMessage: 'No values provided for subscription',
      })
    }

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
      name,
      email,
      status: 'enabled',
      lists: [],
      attribs: {
        language,
        [key]: values,
      },
    }

    let res: Response
    if ('id' in subscriber) {
      res = await fetch(`${listmonk.url}api/subscribers/${subscriber.id}`, {
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
      res = await fetch(`${listmonk.url}api/subscribers`, {
        method: 'POST',
        body: JSON.stringify(subscriber),
        headers: {
          ...authorization,
          'content-type': 'application/json',
        },
      })
    }

    if (!res.ok) {
      const cause = await res.text()
      return createError({
        statusCode: res.status,
        statusMessage: `Failed to subscribe: ${res.statusText}`,
        cause,
      })
    }

    return {
      message: 'Subscription successful',
    }
  }
}
