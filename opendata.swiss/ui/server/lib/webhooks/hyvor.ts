import type Listmonk from '../../api/subscribe/listmonk'
import type { NitroRuntimeConfig } from 'nitropack/types'
import type { HubSearch } from '../../lib/piveau'

export interface User {
  email: string
}

export interface Page {
  url: string
  title: string
  identifier: `dataset-${string}`
}

export interface Comment {
  user: User
  page: Page
  parent: Comment | null
  body_html: string
}

export interface Rating {
  id: number
  created_at: number | null
  page: Page | null
  user: User | null
  rating: number
}

export default class {
  constructor(
    private config: Pick<NitroRuntimeConfig['hyvor'], 'publisherNotificationTemplateId'>,
    private listmonk: Listmonk,
    private piveau: HubSearch,
  ) {
  }

  async handleComment(payload: Comment) {
    if (payload.parent) {
      return
    }

    if (!this.config.publisherNotificationTemplateId) {
      console.log('Publisher notification template ID not configured')
      return
    }

    const datasetId = payload.page.identifier.substring('dataset-'.length)
    const dataset = await this.piveau.datasets.get(datasetId)

    if (dataset instanceof Error) {
      return dataset
    }

    const publisher = dataset.contact_point?.shift()

    if (!publisher?.email) {
      return new Error(`No contact point email found for dataset ${datasetId}`)
    }

    const res = await this.listmonk.transactional.send({
      template_id: this.config.publisherNotificationTemplateId,
      subscriber_email: publisher.email,
      data: {
        page: {
          url: payload.page.url,
          title: payload.page.title,
        },
        publisher: {
          name: publisher.name,
        },
        author: {
          email: payload.user.email,
        },
        comment: {
          body: payload.body_html,
        },
      },
      subscriber_mode: 'external',
    })

    if (!res.ok) {
      return new Error(`Failed to send notification email: ${res.status}`, {
        cause: await res.text(),
      })
    }
  }

  handleRating(payload: Rating) {

  }
}
