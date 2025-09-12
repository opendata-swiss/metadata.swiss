<script setup lang="ts">
import { computed, onMounted, reactive, ref, toRefs, watch } from 'vue'

import { useRoute, useRouter} from 'vue-router'
import { useI18n } from 'vue-i18n'

import type { SearchParamsBase } from '@piveau/sdk-core'
import type { SearchResultFacetGroupLocalized } from '@piveau/sdk-vue';

import { useDatasetsSearch, ACTIVE_FACETS } from '../../app/piveau/search'
import OdsBreadcrumbs, { type BreadcrumbItem } from "../../app/components/OdsBreadcrumbs.vue";
import OdsPagination from "../../app/components/OdsPagination.vue";
import OdsDatasetList from "../../app/components/dataset/OdsDatasetList.vue";
import OdsFilterPanel from "../../app/components/dataset/OdsFilterPanel.vue";
import OdsListCardToggle from "../../app/components/dataset/list-card-toggle/OdsListCardToggle.vue";
import OdsSortSelect from "../../app/components/dataset/OdsSortSelect.vue";
import {homePageBreadcrumb} from "../../app/composables/breadcrumbs";
import SvgIcon from "../../app/components/SvgIcon.vue";
import OdsButton from "../../app/components/OdsButton.vue";

const { t, locale} = useI18n()

const router = useRouter()
const route = useRoute()

// Create a record of refs for each facet in ACTIVE_FACETS
const facetRefs = Object.fromEntries(
  ACTIVE_FACETS.map(facet => [facet, ref<string[]>([])])
);

function syncFacetsFromRoute() {
  ACTIVE_FACETS.forEach(facet => {
    const val = route.query[facet]
    if (val) {
      let arr: string[] = [];
      if (Array.isArray(val)) {
        arr = val.filter((v): v is string => typeof v === 'string');
      } else if (typeof val === 'string' && val.includes(',')) {
        arr = val.split(',').map(v => v.trim()).filter(Boolean);
      } else if (val) {
        arr = [String(val)];
      }
      facetRefs[facet].value = arr;
    } else {
      facetRefs[facet].value = [];
    }
  })
}

function resetSearch() {
  searchInput.value = ''
  ACTIVE_FACETS.forEach(facet => {
    facetRefs[facet].value = []
  })
  piveauQueryParams.page = 0
}

const sortOptions = computed(() => {
  const currentLocale = locale.value;
  return [
    { value: `relevance`, text: t('message.dataset_search.sort_by.relevance') },
    { value: `title.${currentLocale}+asc`, text: t('message.dataset_search.sort_by.title_asc') },
    { value: `title.${currentLocale}+dsc`, text: t('message.dataset_search.sort_by.title_desc') },
    { value: `modified+desc`, text: t('message.dataset_search.sort_by.date_modified_desc') },
    { value: `modified+asc`, text: t('message.dataset_search.sort_by.date_modified_asc') },
  ]
})
const selectedSort = ref<string>(typeof route.query.sort === 'string' ? route.query.sort.replace(/ /g, '+') : '')

watch(selectedSort, (sortString) => {
  // use the route to update the query parameters
  router.push({ query: { ...route.query, sort: sortString } });
})

const piveauQueryParams: SearchParamsBase = reactive({
  limit: 10,
  page: route.query.page ? Number(route.query.page) - 1 : 0,
  q: Array.isArray(route.query.q) ? route.query.q.join(' ') : route.query.q || '',
  sort: 'relevance'
})

const { useSearch} = useDatasetsSearch()
const {
  query,
  isFetching,
  getSearchResultsEnhanced,
  getSearchResultsCount,
  getSearchResultsPagesCount,
  getAvailableFacetsLocalized
} = useSearch({
  queryParams: toRefs(piveauQueryParams),
  selectedFacets: facetRefs,
})

const { suspense } = query

const LIST_TYPE_KEY = 'datasets-list-type';
const getInitialListType = () => {
  const stored = typeof window !== 'undefined' ? window.localStorage.getItem(LIST_TYPE_KEY) : null;
  return stored === 'card' ? 'card' : 'list';
};
const listType = ref<'card' | 'list'>(getInitialListType());

watch(listType, (newType) => {
  if (typeof window !== 'undefined') {
    window.localStorage.setItem(LIST_TYPE_KEY, newType);
  }
});

const availableFacets = getAvailableFacetsLocalized(locale.value);

const activeFacets = computed<SearchResultFacetGroupLocalized[]>(() => {
  const facets = availableFacets.value.filter(f => ACTIVE_FACETS.includes(f.id)).sort((a, b) => a.title.localeCompare(b.title))
  return facets
});

function goToPage(newPage: number | string, query = route.query) {
  const page = newPage ? Number(newPage) : 1
  // Collect all facet values from facetRefs
  const facetsQuery = ACTIVE_FACETS.reduce((acc, facet) => {
    if (facetRefs[facet].value.length > 0) {
      acc[facet] = facetRefs[facet].value
    }
    return acc
  }, {} as Record<string, string[]>)
  router.push({
    name: route.name,
    query: { ...query, ...facetsQuery, page },
  })
  scrollToResults();

}

function scrollToResults() {
  const el = document.getElementById('search-results');
  if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }
}

function scrollOnPaging(event: PointerEvent) {
  const element = event.target as Element;
  if(element && (element.localName === 'svg' || element.localName === 'path' || element.localName === 'a' )) {
    scrollToResults();
    }
  }

const searchInput = ref(route.query.q)
const onSearch = () => goToPage(1, { q: searchInput.value })

const homeBreadcrumb = await homePageBreadcrumb(locale)
const datasetBreadcrumb = computed<BreadcrumbItem>(() => {
  const bc = {
    id: 'datasets',
    title: t('message.header.navigation.datasets'),
    route: '/datasets'
  }
  return bc
})

const resultBreadcrumb = computed<BreadcrumbItem | null>(() => {
  const notFirstPage = route.query.page && Number(route.query.page) > 1
  const hasFacetFilters = Object.keys(facetRefs).some(facet => facetRefs[facet].value.length > 0)
  const hasOtherQueryParams = Object.keys(route.query).length > 1 || (route.query.q && route.query.q !== '')
  if (notFirstPage || hasOtherQueryParams || hasFacetFilters) {

    const resultBc =  {
      id: 'search-results',
      title: t('message.dataset_search.search_results'),
      route
    }
    return resultBc
  }
  return null
})

const breadcrumbs = computed<BreadcrumbItem[]>(() => {
  const lastBreadcrumb = resultBreadcrumb.value
  if (lastBreadcrumb === null) {
    return [
      homeBreadcrumb,
      datasetBreadcrumb.value
    ]
  }
  return  [
    homeBreadcrumb,
    datasetBreadcrumb.value,
    lastBreadcrumb
  ]
})

watch(() => route.query.page, (newPage) => {
  piveauQueryParams.page = newPage ? Number(newPage) - 1 : 0
})

watch(() => route.query, (queryParam) => {
  if (Object.keys(queryParam).length === 0) {
   // query params are empty
   resetSearch()
  } else {
    syncFacetsFromRoute()
  }

})

watch(() => route.query.q, (searchTerm) => {
  if (searchTerm) {
    searchInput.value = Array.isArray(searchTerm) ? searchTerm.join(' ') : searchTerm
  } else {
    searchInput.value = ''
  }
  piveauQueryParams.q = searchInput.value
})

watch(() => route.query.sort, (sortTerm) => {
  if (sortTerm) {
    selectedSort.value = Array.isArray(sortTerm) ? sortTerm.join(' ') : sortTerm
      piveauQueryParams.sort = selectedSort.value

  }
})

onMounted(() => {
  syncFacetsFromRoute()

  ACTIVE_FACETS.forEach(facet => {
    watch(facetRefs[facet], (newVal) => {
      const query = { ...route.query }
      // only set the facet if it has changed
      const hasFacetChanged = JSON.stringify(query[(facet)] ?? []) !== JSON.stringify(newVal)

      if (hasFacetChanged) {
        query[facet] = newVal
        if (query.page && query.page !== '1') {
          query.page = '1'; // Reset page to 1 if facets are restored from route
        }
        router.push({ query })
      }
    })
  })
})

await suspense()

useSeoMeta({
  title: `${t('message.header.navigation.datasets')} | opendata.swiss`,
})

</script>

<template>
  <div>

  <header id="main-header">
    <OdsBreadcrumbs :breadcrumbs="breadcrumbs" />
  </header>

 <main id="main-content">

  <!-- search panel -->
   <section class="section section--default bg--secondary-50">

      <div class="container">

         <h1 class="h1">{{ t('message.dataset_search.search_results') }}</h1>
         <div class="search search--large search--page-result">
            <div class="search__group">
               <input
                id="search-input"
                v-model="searchInput"
                :placeholder="t('message.dataset_search.search_placeholder')"
                type="search"
                :label="t('message.dataset_search.search_placeholder')"
                autocomplete="off"
                class="search"
                @keyup.enter="onSearch"
              >
              <OdsButton
                variant="bare"
                :title="t('message.dataset_search.search_button')"
                size="lg"
                icon="Search"
                icon-only
                @click="onSearch"
              />
            </div>
         </div>
         <div class="search__filters">
           <OdsFilterPanel :facet-refs="facetRefs" :facets="activeFacets" />
         </div>
         <div class="filters__active" />
      </div>
   </section>
   <!-- results -->

   <section id="search-results" class="section section--default">
      <div class="container gap--responsive">
         <div class="search-results search-results--grid" aria-live="polite" aria-busy="false">
            <div class="search-results__header">
              <div class="search-results__header__left"><strong>{{ getSearchResultsCount }}</strong>{{ t('message.dataset_search.search_results') }} </div>
                <div class="search-results__header__right">
                  <OdsSortSelect v-model="selectedSort" :options="sortOptions" />
                  <div class="separator separator--vertical" />
                    <OdsListCardToggle v-model="listType" />
                </div>
              </div>
            <h2 class="sr-only">Results list</h2>
           <!-- <div v-if="isFetching" class="is-fetching">
              Fetching...
            </div>-->
            <OdsDatasetList :items="getSearchResultsEnhanced" :list-type="listType" :search-params="route.query" />
            <div class="pagination pagination--right">
              <OdsPagination
                :current-page="(Number(route.query.page  ?? 1) )"
                :total-pages="getSearchResultsPagesCount"
                :page-label="t('message.ods-pagination.page')"
                :total-pages-label="t('message.ods-pagination.of') + getSearchResultsPagesCount"
                :pagination-items="[
                {
                  icon: 'ChevronLeft',
                  label: t('message.ods-pagination.previous'),
                  link: { name: route.name, query: { ...route.query, page: (Number(route.query.page  ?? 1) - 1) } /*, hash: '#search-results'*/ },
                },
                {
                  icon: 'ChevronRight',
                  label: t('message.ods-pagination.next'),
                  link: { name: route.name, query: { ...route.query, page: (Number(route.query.page ?? 1) + 1) }, /* hash: '#search-results' */ }
                }
              ]"
                @page-change="(page) => goToPage(page)"
                @click="scrollOnPaging($event)"
                />
            </div>

            <div class="notification notification--info">
               <SvgIcon icon="InfoCircle" role="notification" />
               <div class="notification__content">
                  <div class="text--bold">Haben Sie nicht gefunden wonach Sie suchen?</div>
                  <div>Gerne geben wir Ihnen auch persönlich Auskunft. Bitte melden Sie sich
                     via Kontaktformular bei uns.
                  </div>
                  <a href="#" class="link">Kontaktformular</a>
               </div>
            </div>

         </div>
      </div>
   </section>
</main>

</div>

</template>

<style lang="scss" scoped>

</style>
