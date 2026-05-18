import type { NitroRuntimeConfig } from 'nitropack/types'
import type { AppLanguage } from '~/constants/langages'

interface Envelope<T> {
  data: T
}

interface Attribs extends Record<string, unknown> {
  datasets?: string[]
  categories?: string[]
  organisations?: string[]
  language?: AppLanguage
}

export interface Subscriber {
  id: number
  name: string
  email: string
  status: 'enabled' | 'blocklisted'
  lists?: number[]
  attribs?: Attribs
}

interface Subscribers {
  results: Array<Subscriber>
}

export default ({ api, template }: NitroRuntimeConfig['listmonk']) => {
  const authorization = {
    Authorization: `token ${api.user}:${api.token}`,
  }

  const baseUrl = new URL('api/', api.url)

  return {
    subscribers: (() => {
      const url = new URL('subscribers', baseUrl)

      return {
        async get(id: string): Promise<Subscriber> {
          const res = await fetch(new URL(`${id}`, url + '/'), {
            headers: authorization,
          })

          if (!res.ok) {
            console.error(await res.text())
            throw new Error(`Failed to fetch subscriber ${id}: ${res.status} ${res.statusText}`)
          }

          const payload: Envelope<Subscriber> = await res.json()
          return payload.data
        },

        async list({ email }: { email?: string } = {}) {
          const searchUrl = new URL(url)
          if (email) {
            searchUrl.searchParams.set('query', `subscribers.email = '${email}'`)
          }

          const getSubscribers = await fetch(searchUrl, {
            headers: authorization,
          })

          if (!getSubscribers.ok) {
            console.error(await getSubscribers.text())
            throw new Error(`Failed to fetch subscribers: ${getSubscribers.status} ${getSubscribers.statusText}`)
          }

          const subscribers: Envelope<Subscribers> = await getSubscribers.json()

          return subscribers.data.results
        },

        create(subscriber: Omit<Subscriber, 'id' | 'email'>) {
          return fetch(url, {
            method: 'POST',
            body: JSON.stringify(subscriber),
            headers: {
              ...authorization,
              'content-type': 'application/json',
            },
          })
        },

        update(subscriber: Subscriber['id'], patch: Partial<Subscriber>) {
          const subscriberUrl = new URL(`${subscriber}`, url + '/')

          return fetch(subscriberUrl, {
            method: 'PATCH',
            body: JSON.stringify(patch),
            headers: {
              ...authorization,
              'content-type': 'application/json',
            },
          })
        },
      }
    })(),

    transactional: {
      send({ subscriber, language, data }: { subscriber: Subscriber['id'], language: AppLanguage, data: unknown }) {
        const template_id = template.ids[language]

        const transaction = {
          template_id,
          subscriber_id: subscriber,
          data,
        }

        const txUrl = new URL('tx', baseUrl)
        return fetch(txUrl, {
          method: 'POST',
          body: JSON.stringify(transaction),
          headers: {
            ...authorization,
            'content-type': 'application/json',
          },
        })
      },
    },
  }
}
