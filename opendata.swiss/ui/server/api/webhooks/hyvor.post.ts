import crypto from 'node:crypto'
import type { Comment, CommentsApiPage, CommentsApiUser } from '@hyvor/hyvor-talk-base'

interface Rating {
  id: number
  created_at: number | null
  page: CommentsApiPage | null
  user: CommentsApiUser | null
  rating: number
}

interface WebhookPayload<T> {
  event: 'comment.create' | 'rating.created' | 'rating.updated' | 'rating.deleted'
  data: T
}

export default defineEventHandler(async (event) => {
  const { hyvor: { webhook_secret } } = useRuntimeConfig(event)

  const body = await readRawBody(event)
  const headers = getRequestHeaders(event)

  const expectedSignature = headers['x-signature']
  const signature = crypto.createHmac('sha256', webhook_secret)
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

  const payload: WebhookPayload<unknown> = JSON.parse(body.toString())

  switch (payload.event) {
    case 'comment.create':
      handleNewComment(payload as WebhookPayload<Comment>)
      break
    case 'rating.created':
    case 'rating.updated':
      handleRating(payload as WebhookPayload<Rating>)
      break
    default:
      return createError({
        status: 400,
        message: `Unsupported event type "${payload.event}"`,
      })
  }
})

function handleNewComment(payload: WebhookPayload<Comment>) {

}

function handleRating(payload: WebhookPayload<Rating>) {

}
