import { schemaDataset } from '@piveau/sdk-core'
import { dcatApDataset, defineHubSearch } from '@piveau/sdk-vue'
import { getKeywords } from './get-keywords'
import { getOdsFormats } from './get-ods-formats'
import { getOdsCatalogInfo } from './get-ods-catalog-info'
import { getOdsAccrualPeriodicity } from './get-ods-accrual-periodicity'
import { getOdsLicenses } from './get-ods-licenses'

export const facets = ['catalog', 'categories', 'organization', 'format', 'license', 'keywords']

interface SearchResponseFacetItem {
  id: string
  title?: string | Record<string, string>
  count: number
}

interface SearchResponseFacetGroup {
  id: string
  title?: string | Record<string, string>
  items: SearchResponseFacetItem[]
}

interface SearchResponseWithFacets {
  result?: {
    facets?: Array<{
      id: string
      title?: string | Record<string, string>
      items?: Array<SearchResponseFacetItem | null | undefined>
    }>
  }
}

function getFacetInfoFromQueryResponse(context: { qc?: { getQueryData: (queryKey: readonly unknown[]) => unknown }, queryKey?: readonly unknown[] }): SearchResponseFacetGroup[] {
  if (!context.qc || !context.queryKey) {
    return []
  }

  const queryResponse = context.qc.getQueryData(context.queryKey) as SearchResponseWithFacets | undefined
  const availableFacets = queryResponse?.result?.facets ?? []

  return availableFacets.map(facet => ({
    id: facet.id,
    title: facet.title,
    items: (facet.items ?? []).filter((item): item is SearchResponseFacetItem => item !== null && item !== undefined),
  }))
}

/**
 * Returns a piveau hub-search query definition for DCAT-AP datasets
 */
export function useDatasetsSearch() {
  const baseUrl = useRuntimeConfig().public.piveauHubSearchUrl as string

  return defineHubSearch({
    baseUrl,
    index: 'dataset',
    indexDetails: 'datasets',
    facets,
    schema: schemaDataset,
  }, (dataset, context) => {
    const { setup: base } = dcatApDataset()
    const facetInfo = getFacetInfoFromQueryResponse(context)

    return {
      ...base(dataset, context),
      getKeywords: getKeywords(dataset, context),
      getOdsCatalogInfo: getOdsCatalogInfo(dataset, context, facetInfo),
      getOdsFormats: getOdsFormats(dataset),
      getOdsAccrualPeriodicity: getOdsAccrualPeriodicity(dataset),
      getResource: dataset.resource,
      getOdsLicenses: getOdsLicenses(dataset),
      getPublisher: dataset.catalog.publisher,
    }
  })
}
