import z from 'zod'
import { defineHubSearch } from '@piveau/sdk-vue'

export function useVocabularySearch() {
  const baseUrl = useRuntimeConfig().public.piveauHubSearchUrl as string

  return defineHubSearch({
    baseUrl,
    index: 'vocabulary',
    schema: z.object({
      pref_label: z.map(z.string(), z.string()),
      id: z.string(),
      resource: z.string(),
      in_scheme: z.string(),
      index: z.string(),
    }),
  }, (resource, localeInstance) => {
    return {
      ...resource,
      pref_label: resource.pref_label[localeInstance.currentLocale],
    }
  })
}
