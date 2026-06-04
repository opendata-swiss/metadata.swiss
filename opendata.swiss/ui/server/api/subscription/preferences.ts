import Listmonk from '../../lib/listmonk'
import { validatePreferencesToken } from '#server/lib/listmonk/token'

export default defineEventHandler(async (event) => {
  const id = validatePreferencesToken(event)

  const { listmonk: config } = useRuntimeConfig()

  const listmonk = new Listmonk(config)

  const subscriber = await listmonk.subscribers.get(id)

  return {
    preferences: subscriber.attribs || {},
  }
})
