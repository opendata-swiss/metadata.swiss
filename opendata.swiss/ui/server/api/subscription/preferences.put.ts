import type { Frequency } from '../../lib/listmonk'
import listmonk from '../../lib/listmonk'
import type { AppLanguage } from '~/constants/langages'
import { validatePreferencesToken } from '#server/lib/listmonk/token'

export default defineEventHandler(async (event) => {
  const { listmonk: config } = useRuntimeConfig()

  const Listmonk = listmonk(config)

  const id = validatePreferencesToken(event)

  const subscriber = await Listmonk.subscribers.get(id)
  subscriber.attribs = subscriber.attribs || {}
  subscriber.attribs.datasets = []
  subscriber.attribs.categories = []

  const form = await readFormData(event)
  for (const formField of form) {
    switch (formField[0]) {
      case 'dataset':
        subscriber.attribs.datasets.push(formField[1] as string)
        break
      case 'category':
        subscriber.attribs.categories.push(formField[1] as string)
        break
      case 'language':
        subscriber.attribs.language = formField[1] as AppLanguage
        break
      case 'frequency':
        subscriber.attribs.frequency = formField[1] as Frequency
        break
      default:
        console.warn(`Unknown form field: ${formField[0]}`)
        break
    }
  }

  const updated = await Listmonk.subscribers.update(subscriber.id, {
    attribs: subscriber.attribs,
  })
  if (!updated.ok) {
    console.error(await updated.text())
    throw new Error(`Failed to update subscriber ${id}: ${updated.status} ${updated.statusText}`)
  }

  return null
})
