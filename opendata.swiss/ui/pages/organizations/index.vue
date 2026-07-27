<script setup lang="ts">
import { computed, ref, toRefs, watch } from 'vue'
import { useI18n, useRoute, useRouter } from '#imports'
import { useSeoMeta } from 'nuxt/app'

import type { SearchParamsBase } from '@piveau/sdk-core/hubSearch'

import OdsBreadcrumbs, { type BreadcrumbItem } from '../../app/components/OdsBreadcrumbs.vue'
import OdsOrganizationCard from '../../app/components/organizations/OdsOrganizationCard.vue'
import OdsPagination from '../../app/components/OdsPagination.vue'
import OdsSearchPanel from '../../app/components/OdsSearchPanel.vue'
import OdsSearchResults from '../../app/components/OdsSearchResults.vue'
import { homePageBreadcrumb } from '../../app/composables/breadcrumbs'
import { useOrganizationsSearch } from '../../app/piveau/organizations'

const { t, locale } = useI18n()
const route = useRoute()
const router = useRouter()

function queryValue(value: string | string[] | undefined) {
  return Array.isArray(value) ? value.join(' ') : value || ''
}

const piveauQueryParams: SearchParamsBase = reactive({
  limit: 10,
  page: route.query.page ? Number(route.query.page) - 1 : 0,
  q: queryValue(route.query.q),
})

const { useSearch } = useOrganizationsSearch()
const {
  query,
  getSearchResultsEnhanced,
  getSearchResultsCount,
  getSearchResultsPagesCount,
} = useSearch({
  queryParams: toRefs(piveauQueryParams),
})

const organizations = computed(() => getSearchResultsEnhanced.value?.map(organization => ({
  id: organization.getId,
  label: organization.getTitle || '',
  description: organization.getDescription || undefined,
})) || [])

const { suspense } = query
const searchInput = ref(queryValue(route.query.q))

function scrollToResults() {
  document.getElementById('search-results')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function goToPage(newPage: number | string, query = route.query) {
  router.push({
    name: route.name,
    query: { ...query, page: newPage ? Number(newPage) : 1 },
  })
  scrollToResults()
}

function onSearch() {
  goToPage(1, { q: searchInput.value })
}

function scrollOnPaging(event: PointerEvent) {
  const element = event.target as Element
  if (['svg', 'path', 'a'].includes(element?.localName)) {
    scrollToResults()
  }
}

const homeBreadcrumb = await homePageBreadcrumb(locale)
const organizationBreadcrumb = computed<BreadcrumbItem>(() => ({
  id: 'organizations',
  title: t('message.header.navigation.organizations'),
  route: '/organizations',
}))

const resultBreadcrumb = computed<BreadcrumbItem | null>(() => {
  const hasSearchParameters = Object.keys(route.query).length > 0
  const isNotFirstPage = route.query.page && Number(route.query.page) > 1

  if (isNotFirstPage || hasSearchParameters) {
    return {
      id: 'search-results',
      title: t('message.dataset_search.search_results'),
      route,
    }
  }

  return null
})

const breadcrumbs = computed<BreadcrumbItem[]>(() => [
  homeBreadcrumb,
  organizationBreadcrumb.value,
  ...resultBreadcrumb.value ? [resultBreadcrumb.value] : [],
])

watch(() => route.query.page, (newPage) => {
  piveauQueryParams.page = newPage ? Number(newPage) - 1 : 0
})

watch(() => route.query.q, (searchTerm) => {
  searchInput.value = queryValue(searchTerm)
  piveauQueryParams.q = searchInput.value
})

useSeoMeta({
  title: `${t('message.header.navigation.organizations')} | opendata.swiss`,
})

await suspense()
</script>

<template>
  <div>
    <header id="main-header">
      <OdsBreadcrumbs :breadcrumbs="breadcrumbs" />
    </header>

    <main id="main-content">
      <OdsSearchPanel
        :search-input="searchInput"
        :search-prompt="t('message.organization_search.search_placeholder')"
        @search="onSearch"
        @update:search-input="value => searchInput = value"
      />

      <OdsSearchResults :results-count="getSearchResultsCount">
        <ul class="organization-list">
          <li
            v-for="organization in organizations"
            :key="organization.id"
          >
            <OdsOrganizationCard v-bind="organization" />
          </li>
        </ul>

        <div class="pagination pagination--right">
          <OdsPagination
            :current-page="Number(route.query.page ?? 1)"
            :total-pages="getSearchResultsPagesCount"
            :page-label="t('message.ods-pagination.page')"
            :total-pages-label="t('message.ods-pagination.of') + getSearchResultsPagesCount"
            :pagination-items="[
              {
                icon: 'ChevronLeft',
                label: t('message.ods-pagination.previous'),
                link: { name: route.name, query: { ...route.query, page: Number(route.query.page ?? 1) - 1 } },
              },
              {
                icon: 'ChevronRight',
                label: t('message.ods-pagination.next'),
                link: { name: route.name, query: { ...route.query, page: Number(route.query.page ?? 1) + 1 } },
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
.organization-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.organization-list > li + li {
  margin-top: 1rem;
}
</style>
