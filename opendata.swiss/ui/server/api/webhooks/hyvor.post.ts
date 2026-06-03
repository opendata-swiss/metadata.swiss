import crypto from 'node:crypto'
import type { Comment, Rating } from '#server/lib/webhooks/hyvor'
import Hyvor from '#server/lib/webhooks/hyvor'
import Listmonk from '#server/lib/listmonk'
import { HubSearch } from '#server/lib/piveau'

interface RatingWebhookPayload {
  event: 'rating.created' | 'rating.updated' | 'rating.deleted'
  data: Rating
}

interface CommentWebhookPayload {
  event: 'comment.create'
  data: Comment
}

type WebhookPayload = RatingWebhookPayload | CommentWebhookPayload

export default defineEventHandler(async (event) => {
  const { listmonk, hyvor: config, public: { piveauHubSearchUrl } } = useRuntimeConfig(event)

  const body = await readRawBody(event)
  const headers = getRequestHeaders(event)

  const expectedSignature = headers['x-signature']
  const signature = crypto.createHmac('sha256', config.webhookSecret)
    .update(body!)
    .digest('hex')

  if (!expectedSignature || expectedSignature !== signature) {
    return createError({
      status: 400,
      message: 'Signature mismatch',
    })
  }

  if (!body) {
    return createError({
      status: 400,
      message: 'Missing body',
    })
  }

  if (config.webhooksEnabled === false) {
    return 204
  }

  const payload: WebhookPayload = JSON.parse(body.toString())
  const hyvor = new Hyvor(config, new Listmonk(listmonk), new HubSearch(piveauHubSearchUrl))

  switch (payload.event) {
    case 'comment.create':
      return hyvor.handleComment(payload.data)
    case 'rating.created':
    case 'rating.updated':
      return hyvor.handleRating(payload.data)
    default:
      return createError({
        status: 400,
        message: `Unsupported event type "${payload.event}"`,
      })
  }
})
