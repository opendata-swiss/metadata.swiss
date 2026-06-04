import Listmonk from '../../lib/listmonk'
import { validatePreferencesToken } from '#server/lib/listmonk/token'

export default defineEventHandler(async (event) => {
  const { listmonk: config } = useRuntimeConfig()

  const id = validatePreferencesToken(event, config.preferences.hmac_key)

  const listmonk = new Listmonk(config)

  const subscriber = await listmonk.subscribers.get(id)

  return {
    preferences: subscriber.attribs || {},
  }
})
