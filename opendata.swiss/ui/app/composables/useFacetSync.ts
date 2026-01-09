import { watch } from 'vue'
import type { LocationQuery, LocationQueryValue } from 'vue-router'
import type { useRoute, useRouter } from '#vue-router'

type FacetRefs<F extends string> = Record<F, Ref<string[]>>

interface SyncFacetsFromRouteArgs {
  facets: string[]
  facetRefs: Record<string, Ref<LocationQueryValue[]>>
  route: ReturnType<typeof useRoute>
}

export function syncFacetsFromRoute({ facets, facetRefs, route }: SyncFacetsFromRouteArgs) {
  facets.forEach((facet) => {
    const newVal = route.query[facet] || []
    facetRefs[facet]!.value = Array.isArray(newVal) ? newVal : [newVal]
  })
}

interface UseFacetSyncArgs<F extends string> {
  facets: F[]
  facetRefs: FacetRefs<F>
  route: ReturnType<typeof useRoute>
  router: ReturnType<typeof useRouter>
}

export function useFacetSync<F extends string>({
  facets,
  facetRefs,
  route,
  router,
}: UseFacetSyncArgs<F>) {
  const hasFacetChanged = (query: LocationQuery, facet: string, newVal: string[]) => {
    const current = Array.isArray(query[facet]) ? query[facet] : query[facet] ? [query[facet]] : []
    const currentValues = new Set(current)
    return newVal.length !== currentValues.size || newVal.some(value => !currentValues.has(value))
  }

  facets.forEach((facet) => {
    watch(facetRefs[facet], (newVal) => {
      const query = { ...route.query }
      if (!hasFacetChanged(route.query, facet, newVal)) {
        return
      }

      query[facet] = newVal
      if (query.page && query.page !== '1') {
        query.page = '1'
      }

      router.push({ query })
    })
  })
}
