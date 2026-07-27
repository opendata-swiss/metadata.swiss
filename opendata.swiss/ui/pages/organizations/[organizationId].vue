<script setup lang="ts">
import { computed, onMounted, reactive, ref, toRefs, watch, type Ref } from 'vue'
import type { LocationQueryRaw, LocationQueryValue } from 'vue-router'
import { createError, useI18n, useRoute, useRouter } from '#imports'
import { useSeoMeta } from 'nuxt/app'

import type { SearchParamsBase } from '@piveau/sdk-core/hubSearch'
import type { SearchResultFacetGroupLocalized } from '@piveau/sdk-vue'

import OdsBreadcrumbs, { type BreadcrumbItem } from '../../app/components/OdsBreadcrumbs.vue'
import OdsPagination from '../../app/components/OdsPagination.vue'
import OdsSearchPanel from '../../app/components/OdsSearchPanel.vue'
import OdsSearchResults from '../../app/components/OdsSearchResults.vue'
import OdsDatasetList from '../../app/components/dataset/OdsDatasetList.vue'
import OdsListCardToggle from '../../app/components/dataset/list-card-toggle/OdsListCardToggle.vue'
import OdsSortSelect from '../../app/components/dataset/OdsSortSelect.vue'
import { DcatApChV2DatasetAdapter } from '../../app/components/dataset-detail/model/dcat-ap-ch-v2-dataset-adapter'
import { homePageBreadcrumb } from '../../app/composables/breadcrumbs'
import { useOrganizationsSearch } from '../../app/piveau/organizations'
import { useDatasetsSearch, facets as datasetFacets } from '../../app/piveau/datasets'

const { t, locale } = useI18n()
const route = useRoute()
const router = useRouter()

const organizationId = computed(() => {
  const id = route.params.organizationId
  return Array.isArray(id) ? id[0] || '' : id || ''
})
const organizationPath = computed(() => `/organizations/${encodeURIComponent(organizationId.value)}`)

const { useResource } = useOrganizationsSearch()
const { query: organizationQuery, resultEnhanced: organization } = useResource(organizationId)

try {
  await organizationQuery.suspense()
}
catch (error) {
  if ((error as { response?: { status?: number } }).response?.status === 404) {
    throw createError({ status: 404, statusText: 'Page Not Found', fatal: true })
  }

  throw error
}

if (!organization.value) {
  throw createError({ status: 404, statusText: 'Page Not Found', fatal: true })
}

function queryValue(value: LocationQueryValue | LocationQueryValue[] | undefined) {
  return Array.isArray(value) ? value.filter((item): item is string => typeof item === 'string').join(' ') : value || ''
}

function queryValues(value: LocationQueryValue | LocationQueryValue[] | undefined) {
  if (Array.isArray(value)) {
    return value.filter((item): item is string => typeof item === 'string')
  }

  return typeof value === 'string' ? [value] : []
}

function currentPage() {
  const page = Number(queryValue(route.query.page))
  return Number.isInteger(page) && page > 0 ? page : 1
}

function equalValues(left: string[], right: string[]) {
  return left.length === right.length && left.every((value, index) => value === right[index])
}

const visibleFacetIds = datasetFacets
const selectedFacets = reactive<Record<string, string[]>>(
  Object.fromEntries(visibleFacetIds.map(facet => [facet, queryValues(route.query[facet])])) as Record<string, string[]>,
)

const facetRefs = Object.fromEntries(
  visibleFacetIds.map(facet => [facet, computed({
    get: () => selectedFacets[facet] || [],
    set: (value: string[]) => { selectedFacets[facet] = value },
  })]),
) as Record<string, Ref<string[]>>

const piveauQueryParams: SearchParamsBase = reactive({
  limit: 10,
  page: currentPage() - 1,
  q: queryValue(route.query.q),
  sort: queryValue(route.query.sort) || 'relevance',
})

const selectedSort = ref(queryValue(route.query.sort) || 'relevance')
const organizationFacet = computed({
  get: () => [organizationId.value],
  set: () => {},
})
const searchFacetRefs: Record<string, Ref<string[]>> = {
  ...facetRefs,
  publisher: organizationFacet,
}

const { useSearch } = useDatasetsSearch()
const {
  query: datasetsQuery,
  getSearchResultsEnhanced,
  getSearchResultsCount,
  getSearchResultsPagesCount,
  getAvailableFacetsLocalized,
} = useSearch({
  queryParams: toRefs(piveauQueryParams),
  selectedFacets: searchFacetRefs,
})

const datasets = computed(() => getSearchResultsEnhanced.value?.map(item => new DcatApChV2DatasetAdapter(item)))
const searchInput = ref(queryValue(route.query.q))
const availableFacets = getAvailableFacetsLocalized(locale.value)

const LIST_TYPE_KEY = 'datasets-list-type'
const getInitialListType = () => {
  const stored = typeof window !== 'undefined' ? window.localStorage.getItem(LIST_TYPE_KEY) : null
  return stored === 'card' ? 'card' : 'list'
}
const listType = ref<'card' | 'list'>(getInitialListType())

watch(listType, (newType) => {
  if (typeof window !== 'undefined') {
    window.localStorage.setItem(LIST_TYPE_KEY, newType)
  }
})

const activeFacets = computed<SearchResultFacetGroupLocalized[]>(() => {
  return availableFacets.value
    .filter(facet => visibleFacetIds.includes(facet.id))
    .sort((left, right) => left.title.localeCompare(right.title))
})

const sortOptions = computed(() => {
  const currentLocale = locale.value
  return [
    { value: 'relevance', text: t('message.dataset_search.sort_by.relevance') },
    { value: `title.${currentLocale}+asc`, text: t('message.dataset_search.sort_by.title_asc') },
    { value: `title.${currentLocale}+dsc`, text: t('message.dataset_search.sort_by.title_desc') },
    { value: 'modified+desc', text: t('message.dataset_search.sort_by.date_modified_desc') },
    { value: 'modified+asc', text: t('message.dataset_search.sort_by.date_modified_asc') },
  ]
})

interface QueryOverrides {
  q?: string
  facets?: Record<string, string[]>
  sort?: string
  page?: number
}

function serializeQuery(overrides: QueryOverrides = {}): LocationQueryRaw {
  const q = overrides.q ?? queryValue(route.query.q)
  const sort = overrides.sort ?? selectedSort.value
  const page = overrides.page ?? currentPage()
  const query: LocationQueryRaw = {}

  if (q) {
    query.q = q
  }
  visibleFacetIds.forEach((facet) => {
    const values = overrides.facets?.[facet] ?? facetRefs[facet]?.value ?? []
    if (values.length > 0) {
      query[facet] = values
    }
  })
  if (sort !== 'relevance') {
    query.sort = sort
  }
  if (page > 1) {
    query.page = String(page)
  }

  return query
}

function navigate(query: LocationQueryRaw, replace = false) {
  const location = { path: route.path, query }
  return replace ? router.replace(location) : router.push(location)
}

function resetAllFacets() {
  navigate(serializeQuery({
    facets: Object.fromEntries(visibleFacetIds.map(facet => [facet, []])),
    page: 1,
  }))
}

function onSearch() {
  navigate(serializeQuery({ q: searchInput.value.trim(), page: 1 }))
}

function goToPage(page: number | string) {
  navigate(serializeQuery({ page: page ? Number(page) : 1 }))
  scrollToResults()
}

function scrollToResults() {
  document.getElementById('search-results')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function scrollOnPaging(event: PointerEvent) {
  const element = event.target as Element
  if (['svg', 'path', 'a'].includes(element?.localName)) {
    scrollToResults()
  }
}

const homeBreadcrumb = await homePageBreadcrumb(locale)
const organizationTitle = computed(() => organization.value?.getTitle || '')
const organizationDescription = computed(() => organization.value?.getDescription || '')
const breadcrumbs = computed<BreadcrumbItem[]>(() => [
  homeBreadcrumb,
  {
    id: 'organizations',
    title: t('message.header.navigation.organizations'),
    route: '/organizations',
  },
  {
    id: 'organization',
    title: organizationTitle.value,
    route: organizationPath.value,
  },
])

watch(() => route.query.page, () => {
  piveauQueryParams.page = currentPage() - 1
})

watch(() => route.query.q, (searchTerm) => {
  searchInput.value = queryValue(searchTerm)
  piveauQueryParams.q = searchInput.value
})

visibleFacetIds.forEach((facet) => {
  watch(() => route.query[facet], (queryFacet) => {
    const values = queryValues(queryFacet)
    if (!equalValues(values, facetRefs[facet]!.value)) {
      facetRefs[facet]!.value = values
    }
  })

  watch(facetRefs[facet]!, (values) => {
    if (!equalValues(values, queryValues(route.query[facet]))) {
      navigate(serializeQuery({ facets: { [facet]: values }, page: 1 }))
    }
  })
})

watch(() => route.query.sort, (sort) => {
  const value = queryValue(sort) || 'relevance'
  piveauQueryParams.sort = value
  if (selectedSort.value !== value) {
    selectedSort.value = value
  }
})

watch(selectedSort, (sort) => {
  piveauQueryParams.sort = sort
  if (sort !== (queryValue(route.query.sort) || 'relevance')) {
    navigate(serializeQuery({ sort, page: 1 }))
  }
})

onMounted(() => {
  const allowedQueryParameters = new Set(['q', 'sort', 'page', ...visibleFacetIds])
  if (Object.keys(route.query).some(key => !allowedQueryParameters.has(key))) {
    navigate(serializeQuery(), true)
  }
})

useSeoMeta({
  title: () => `${organizationTitle.value} | ${t('message.header.navigation.organizations')} | opendata.swiss`,
})

await datasetsQuery.suspense()
</script>

<template>
  <div>
    <header id="main-header">
      <OdsBreadcrumbs :breadcrumbs="breadcrumbs" />
    </header>

    <main id="main-content">
      <section class="section section--default">
        <div class="container">
          <h1 class="h1">
            {{ organizationTitle }}
          </h1>
          <p
            v-if="organizationDescription"
            class="organization-description"
          >
            {{ organizationDescription }}
          </p>
        </div>
      </section>

      <OdsSearchPanel
        :search-input="searchInput"
        :search-prompt="t('message.dataset_search.search_placeholder')"
        :facet-refs="facetRefs"
        :active-facets="activeFacets"
        @search="onSearch"
        @reset-all-facets="resetAllFacets"
        @update:search-input="value => searchInput = value"
      />

      <OdsSearchResults :results-count="getSearchResultsCount">
        <template #header-right>
          <OdsSortSelect
            v-model="selectedSort"
            :options="sortOptions"
          />
          <div class="separator separator--vertical" />
          <OdsListCardToggle v-model="listType" />
        </template>

        <OdsDatasetList
          :items="datasets"
          :list-type="listType"
          :search-params="serializeQuery()"
        />

        <div class="pagination pagination--right">
          <OdsPagination
            :current-page="currentPage()"
            :total-pages="getSearchResultsPagesCount"
            :page-label="t('message.ods-pagination.page')"
            :total-pages-label="t('message.ods-pagination.of') + getSearchResultsPagesCount"
            :pagination-items="[
              {
                icon: 'ChevronLeft',
                label: t('message.ods-pagination.previous'),
                link: { path: organizationPath, query: serializeQuery({ page: currentPage() - 1 }) },
              },
              {
                icon: 'ChevronRight',
                label: t('message.ods-pagination.next'),
                link: { path: organizationPath, query: serializeQuery({ page: currentPage() + 1 }) },
              },
            ]"
            @page-change="goToPage"
            @click="scrollOnPaging($event)"
          />
        </div>
      </OdsSearchResults>
    </main>
  </div>
</template>

<style scoped lang="scss">
.organization-description {
  margin-bottom: 0;
}
</style>
