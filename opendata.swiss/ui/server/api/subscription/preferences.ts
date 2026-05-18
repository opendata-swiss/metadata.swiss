import listmonk from '../../lib/listmonk'
import { validatePreferencesToken } from '#server/lib/listmonk/token'

export default defineEventHandler(async (event) => {
  const id = validatePreferencesToken(event)

  const { listmonk: config } = useRuntimeConfig()

  const Listmonk = listmonk(config)

  const subscriber = await Listmonk.subscribers.get(id)

  return {
    preferences: subscriber.attribs || {},
  }
})
