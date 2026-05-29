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

interface TransactionBase {
  template_id: number
  data: unknown
  subscriber_mode?: 'external' | 'fallback' | 'default'
}

interface SubscriberById {
  subscriber_id: Subscriber['id']
}

interface SubscriberByEmail {
  subscriber_email: Subscriber['email']
}

type Transaction = TransactionBase & (SubscriberById | SubscriberByEmail)

export type ListmonkConfig = NitroRuntimeConfig['listmonk']

export default class {
  private readonly authorization: HeadersInit
  private readonly baseUrl: URL

  constructor(private config: ListmonkConfig) {
    this.authorization = {
      Authorization: `token ${config.api.user}:${config.api.token}`,
    }

    this.baseUrl = new URL('api/', config.api.url)
  }

  get subscribers() {
    const url = new URL('subscribers', this.baseUrl)

    return {
      list: async ({ email }: { email?: string } = {}) => {
        const searchUrl = new URL(url)
        if (email) {
          searchUrl.searchParams.set('query', `subscribers.email = '${email}'`)
        }

        const getSubscribers = await fetch(searchUrl, {
          headers: this.authorization,
        })

        if (!getSubscribers.ok) {
          throw new Error(`Failed to fetch subscribers: ${getSubscribers.status} ${getSubscribers.statusText}`)
        }

        const subscribers: Envelope<Subscribers> = await getSubscribers.json()

        return subscribers.data.results
      },

      create: (subscriber: Omit<Subscriber, 'id' | 'email'>) => {
        return fetch(url, {
          method: 'POST',
          body: JSON.stringify(subscriber),
          headers: {
            ...this.authorization,
            'content-type': 'application/json',
          },
        })
      },

      update: (subscriber: Subscriber['id'], patch: Partial<Subscriber>) => {
        const subscriberUrl = new URL(`${subscriber}`, url + '/')

        return fetch(subscriberUrl, {
          method: 'PATCH',
          body: JSON.stringify(patch),
          headers: {
            ...this.authorization,
            'content-type': 'application/json',
          },
        })
      },
    }
  }

  get transactional() {
    return {
      sendDigest: ({ subscriber, language, data }: { subscriber: Subscriber['id'], language: AppLanguage, data: unknown }) => {
        const template_id = this.config.template.ids[language]

        const transaction = {
          template_id,
          subscriber_id: subscriber,
          data,
        }

        return this.transactional.send(transaction)
      },

      send: (transaction: Transaction) => {
        const txUrl = new URL('tx', this.baseUrl)
        return fetch(txUrl, {
          method: 'POST',
          body: JSON.stringify(transaction),
          headers: {
            ...this.authorization,
            'content-type': 'application/json',
          },
        })
      },
    }
  }
}
