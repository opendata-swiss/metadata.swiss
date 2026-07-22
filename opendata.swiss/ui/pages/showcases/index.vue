<script setup lang="ts">
import { computed, onMounted, reactive, ref, toRefs, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from '#imports'
import { useSeoMeta } from 'nuxt/app'
import type { SearchParamsBase } from '@piveau/sdk-core/hubSearch'

import OdsPage from '../../app/components/OdsPage.vue'
import { homePageBreadcrumb } from '../../app/composables/breadcrumbs.js'
import OdsBreadcrumbs from '../../app/components/OdsBreadcrumbs.vue'
import { useShowcaseSearch, facets } from '../../app/piveau/showcases'
import type { SearchResultFacetGroupLocalized } from '@piveau/sdk-vue'
import OdsSearchPanel from '../../app/components/OdsSearchPanel.vue'
import OdsSearchResults from '../../app/components/OdsSearchResults.vue'
import { syncFacetsFromRoute, useFacetSync } from '../../app/composables/useFacetSync'
import OdsSortSelect from '../../app/components/dataset/OdsSortSelect.vue'
import { useSorting } from '../../app/composables/sort'
import OdsShowcaseCard from '../../app/components/showcases/OdsShowcaseCard.vue'
import OdsButton from '../../app/components/OdsButton.vue'

const { locale, t } = useI18n()

const localePath = useLocalePath()
const route = useRoute()
const router = useRouter()

const searchInput = ref(route.query.q)

// 1. Main reactive object for your logic/UI
const selectedFacets = reactive(
  Object.fromEntries(facets.map(facet => [facet, [] as string[]])),
)

// 2. facetRefs for useSearch API (syncs with selectedFacets)
const facetRefs = Object.fromEntries(
  facets.map(facet => [facet, computed({
    get: () => selectedFacets[facet],
    set: (val: string[]) => { selectedFacets[facet] = val },
  })]),
)

// 3. Use selectedFacets everywhere in your code and UI
function resetAllFacets() {
  for (const key in selectedFacets) {
    selectedFacets[key] = []
  }
  // Reset the 'facets' query parameter
  const query = { ...route.query }
  if (query.page && query.page !== '1') {
    query.page = '1' // Reset page to 1 if facets are restored from route
  }
  query['facets'] = encodeURIComponent(JSON.stringify({}))
  router.push({ query })
}

const onSearch = () => goToPage(1, { q: searchInput.value })

function goToPage(newPage: number | string, query = route.query) {
  const page = newPage ? Number(newPage) : 1
  // Collect all facet values from facetRefs
  const facetsQuery = facets.reduce((acc, facet) => {
    if (facetRefs[facet].value.length > 0) {
      acc[facet] = facetRefs[facet].value
    }
    return acc
  }, {} as Record<string, string[]>)
  router.push({
    name: route.name,
    query: { ...query, ...facetsQuery, page },
  })
  scrollToResults()
}

function scrollToResults() {
  const el = document.getElementById('search-results')
  if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }
}

const initialSort = 'modified+desc'
const piveauQueryParams: SearchParamsBase = reactive({
  limit: 10,
  page: route.query.page ? Number(route.query.page) - 1 : 0,
  q: Array.isArray(route.query.q) ? route.query.q.join(' ') : route.query.q || '',
  sort: initialSort,
})

const { useSearch } = useShowcaseSearch()
const {
  query,
  getSearchResultsEnhanced,
  getAvailableFacetsLocalized,
  getSearchResultsCount,

} = useSearch({
  queryParams: toRefs(piveauQueryParams),
  additionalParams: {
    resource: 'showcase',
  },
  selectedFacets: facetRefs,
})

const availableFacets = getAvailableFacetsLocalized(locale.value)

const activeFacets = computed<SearchResultFacetGroupLocalized[]>(() => {
  return availableFacets.value.filter(f => facets.includes(f.id)).sort((a, b) => a.title.localeCompare(b.title))
})

const breadcrumbs = [
  await homePageBreadcrumb(locale),
  {
    title: t('message.header.navigation.showcases'),
    path: '/showcases',
  },
]
useSeoMeta({
  title: `${t('message.header.navigation.showcases')} | opendata.swiss`,
})

function resetSearch() {
  searchInput.value = ''
  facets.forEach((facet) => {
    facetRefs[facet].value = []
  })
  piveauQueryParams.page = 0
}

watch(() => route.query.page, (newPage) => {
  piveauQueryParams.page = newPage ? Number(newPage) - 1 : 0
})

watch(() => route.query, (queryParam) => {
  if (Object.keys(queryParam).length === 0) {
    // query params are empty
    resetSearch()
  }
  else {
    // syncFacetsFromRoute()
  }
})

watch(() => route.query.q, (searchTerm) => {
  if (searchTerm) {
    searchInput.value = Array.isArray(searchTerm) ? searchTerm.join(' ') : searchTerm
  }
  else {
    searchInput.value = ''
  }
  piveauQueryParams.q = searchInput.value
})

onMounted(() => {
  syncFacetsFromRoute({
    facets,
    facetRefs,
    route,
  })

  useFacetSync({
    facets,
    facetRefs,
    route,
    router,
  })
})

await query.suspense()

const { selectedSort } = useSorting({
  initialSort,
  sortCallback(sortTerm) {
    selectedSort.value = Array.isArray(sortTerm) ? sortTerm.join(' ') : sortTerm
    piveauQueryParams.sort = selectedSort.value
  },
})

const sortOptions = computed(() => {
  return [
    { value: `title.${locale.value}+asc`, text: t('message.showcase.search.sort_by.title_asc') },
    { value: `title.${locale.value}+dsc`, text: t('message.showcase.search.sort_by.title_desc') },
    { value: 'modified+desc', text: t('message.showcase.search.sort_by.newest') },
    { value: 'modified+asc', text: t('message.showcase.search.sort_by.oldest') },
    { value: 'issued+desc', text: t('message.showcase.search.sort_by.issued_desc') },
    { value: 'issued+asc', text: t('message.showcase.search.sort_by.issued_asc') },
  ]
})
</script>

<template>
  <OdsPage :hero="{ image: 'https://picsum.photos/1192/894/?image=29', title: t('message.showcase.hero.title') }">
    <template #hero-content>
      <p>
        {{ t('message.showcase.hero.text') }}
      </p>
      <br>
      <p>
        <OdsButton
          variant="outline"
          :href="localePath('/showcases/submit')"
        >
          {{ t('message.header.navigation.showcase_submit') }}
        </OdsButton>
      </p>
    </template>
    <template #header>
      <OdsBreadcrumbs :breadcrumbs="breadcrumbs" />
    </template>
    <!-- search panel -->
    <OdsSearchPanel
      :search-input="searchInput"
      :search-prompt="t('message.showcase.search.prompt')"
      :facet-refs="facetRefs"
      :active-facets="activeFacets"
      @search="onSearch"
      @reset-all-facets="resetAllFacets"
      @update:search-input="value => searchInput = value"
    />
    <!-- results -->
    <OdsSearchResults :results-count="getSearchResultsCount">
      <template #header-right>
        <OdsSortSelect
          v-model="selectedSort"
          :options="sortOptions"
        />
      </template>
      <div class="ods-card-list">
        <ul class="search-results-list">
          <li
            v-for="showcase in getSearchResultsEnhanced"
            :key="showcase.id"
          >
            <OdsShowcaseCard :showcase="showcase" />
          </li>
        </ul>
      </div>
    </OdsSearchResults>
  </OdsPage>
</template>

<style lang="scss" scoped>
ol, ul {
    list-style: none !important;
    margin: 0 !important;
    padding: 0 !important;
}

.ods-card-list {
        margin-top: 2.5rem !important;
}

@media (min-width: 1024px) {
  .ods-card-list {
        margin-top: 3rem !important;
  }
}
@media (min-width: 640px) {
  .ods-card-list {
        margin-top: 2.25rem !important;
  }
}
@media (min-width: 480px) {
    .ods-card-list {
        margin-top: 1.75rem !important;
  }
}

.tag:not(:last-child)::after {
    content: ', ';
}
</style>
