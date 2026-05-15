import listmonk from '../../lib/listmonk'

export default defineEventHandler(async (event) => {
  const { listmonk: config } = useRuntimeConfig()

  const query = getQuery(event)

  const Listmonk = listmonk(config)

  const subscriber = await Listmonk.subscribers.get(query.id as string)

  return {
    preferences: subscriber.attribs,
  }
})
