import z from 'zod'
import { schemaDataset, } from '@piveau/sdk-core'
import { dcatApDataset, defineHubSearch } from '@piveau/sdk-vue'
import { getKeywords } from './get-keywords'
import { getOdsFormats } from './get-ods-formats'
import { getOdsCatalogInfo } from './get-ods-catalog-info'
import { getOdsAccrualPeriodicity } from './get-ods-accrual-periodicity'

// export const ACTIVE_FACETS = ['categories', 'publisher', 'catalog', 'format', 'license', 'keywords']
export const ACTIVE_FACETS = ['catalog', 'categories', 'publisher', 'format', 'license', 'keywords']

/**
 * Returns a piveau hub-search query definition for DCAT-AP datasets
 */
export function useDatasetsSearch() {
  const baseUrl = useRuntimeConfig().public.piveauHubSearchUrl as string

  return defineHubSearch({
    baseUrl,
    index: 'dataset',
    indexDetails: 'datasets',
    facets: ACTIVE_FACETS,
    schema: schemaDataset,
  }, (dataset, localeInstance) => {
    const { setup: base } = dcatApDataset()


    return {
      ...base(dataset, localeInstance),
      getKeywords: getKeywords(dataset, localeInstance),
      getOdsCatalogInfo: getOdsCatalogInfo(dataset, localeInstance),
      getOdsFormats: getOdsFormats(dataset),
      getOdsAccrualPeriodicity: getOdsAccrualPeriodicity(dataset)
    }
  })
}

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


export const ACTIVE_SHOWCASE_FACETS = ['categories', 'keywords', 'type'];

export function useShowcaseSearch() {
  const baseUrl = useRuntimeConfig().public.piveauHubSearchUrl as string

  return defineHubSearch({
    baseUrl,
    index: 'resource_showcase',
    facets: ACTIVE_SHOWCASE_FACETS,
    schema: z.object({
      image: z.array(z.string()),
      references: z.array(z.any()),
      keywords: z.array(
        z.object({
          id: z.string(),
          label: z.string(),
          language: z.string()
        })
      ),
      catalog: z.object({
        id: z.string(),
        description: z.record(z.string(), z.string()),
        issued: z.string(),
        title: z.record(z.string(), z.string()),
        publisher: z.object({
          type: z.string(),
          name: z.string(),
          email: z.string(),
          homepage: z.string()
        }),
        modified: z.string(),
        homepage: z.string()
      }),
      index: z.string(),
      id: z.string(),
      abstract: z.record(z.string(), z.string()),
      text: z.record(z.string(), z.string()),
      categories: z.array(
        z.object({
          id: z.string(),
          label: z.record(z.string(), z.string()),
          resource: z.string()
        })
      ),
      title: z.record(z.string(), z.string()),
      catalog_record: z.object({
        modified: z.string(),
        issued: z.string()
      })
    }),
  }, (showcase) => {
    return {
      ...showcase,
    }
  })
}