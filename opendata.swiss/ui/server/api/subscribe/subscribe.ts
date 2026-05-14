import type { H3Event } from 'h3'
import listmonk from './listmonk'
import { getLanguage } from '#server/lib/locale'

export function subscribe(key: 'categories' | 'datasets' | 'organisations', queryParam: 'category' | 'organisation' | 'dataset') {
  return async (event: H3Event) => {
    const { user: { email } } = event.context
    const query = getQuery(event)
    const values = Array.isArray(query[queryParam]) ? query[queryParam] : [query[queryParam]]
    const language = getLanguage(event)

    const { listmonk: config } = useRuntimeConfig()

    const Listmonk = listmonk(config)
    const subscribers = await Listmonk.subscribers.list({ email })

    const subscriber = subscribers.pop() || {
      name: '',
      status: 'enabled',
      lists: [],
      attribs: {
        language,
        [key]: values,
      },
    }

    if ('id' in subscriber) {
      await Listmonk.subscribers.update(subscriber.id, {
        attribs: {
          [key]: [...new Set([
            ...(subscriber.attribs?.[key] || []),
            ...values,
          ])],
        },
      })
    }
    else {
      await Listmonk.subscribers.create(subscriber)
    }

    return {
      message: 'Subscription successful',
    }
  }
}
