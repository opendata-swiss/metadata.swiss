import z from 'zod'
import { schemaDataset, } from '@piveau/sdk-core'
import { dcatApDataset, defineHubSearch } from '@piveau/sdk-vue'
import { getKeywords } from './get-keywords'
import { getOdsFormats } from './get-ods-formats'
import { getOdsCatalogInfo } from './get-ods-catalog-info'
import { getOdsAccrualPeriodicity } from './get-ods-accrual-periodicity'
import { PIVEAU_HUB_SEARCH_URL } from '~/constants/piveau-endpoints'

// export const ACTIVE_FACETS = ['categories', 'publisher', 'catalog', 'format', 'license', 'keywords']
export const ACTIVE_FACETS = ['catalog', 'categories', 'publisher', 'format', 'license', 'keywords']

/**
 * Returns a piveau hub-search query definition for DCAT-AP datasets
 */
export function useDatasetsSearch() {
  return defineHubSearch({
    baseUrl: PIVEAU_HUB_SEARCH_URL,
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
  return defineHubSearch({
    baseUrl: 'https://piveau-hub-search-ln.zazukoians.org/',
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

