import listmonk from '../../lib/listmonk'
import type { H3Event } from 'h3'
import crypto from 'node:crypto'

export default defineEventHandler(async (event) => {
  const id = validatePreferencesToken(event)

  const { listmonk: config } = useRuntimeConfig()

  const query = getQuery(event)

  const Listmonk = listmonk(config)

  const subscriber = await Listmonk.subscribers.get(id)

  return {
    preferences: subscriber.attribs || {},
  }
})

export function validatePreferencesToken(event: H3Event) {
  const { token, id: subscriber } = getQuery(event)

  if (!token) {
    throw createError({
      statusCode: 400,
      message: 'Token is required',
    })
  }

  if (!subscriber) {
    throw createError({
      statusCode: 400,
      message: 'Subscriber ID is required',
})
  }

  const { listmonk: { preferences: { hmac_key } } } = useRuntimeConfig()

  const expected = crypto.createHmac('sha256', hmac_key)
    .update(subscriber.toString())
    .digest('hex')

  if (token !== expected) {
    throw createError({
      statusCode: 403,
      message: 'Invalid token',
    })
  }

  return subscriber.toString()
}
