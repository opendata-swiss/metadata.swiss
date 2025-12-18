<script setup lang="ts">
import { computed, reactive, ref, toRefs, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useI18n } from '#imports';
import { useSeoMeta } from 'nuxt/app';
import { getCurrentTranslation } from "../../app/lib/getCurrentTranslation";
import type { SearchParamsBase } from '@piveau/sdk-core';


import OdsPage from "../../app/components/OdsPage.vue";
import {homePageBreadcrumb} from "../../app/composables/breadcrumbs.js";
import OdsBreadcrumbs from "../../app/components/OdsBreadcrumbs.vue";
import OdsCard from "../../app/components/OdsCard.vue";
import SvgIcon from "../../app/components/SvgIcon.vue";
import OdsButton from '../../app/components/OdsButton.vue';
import OdsFilterPanel from '../../app/components/dataset/OdsFilterPanel.vue';
import { useShowcaseSearch, ACTIVE_SHOWCASE_FACETS} from '../../app/piveau/search';
import type { SearchResultFacetGroupLocalized } from '@piveau/sdk-vue';
import OdsSearchPanel from "../../app/components/OdsSearchPanel.vue";
import OdsSearchResults from "../../app/components/OdsSearchResults.vue";

const { locale, t } = useI18n()

const route = useRoute();
const router = useRouter();

const searchInput = ref(route.query.q)


// 1. Main reactive object for your logic/UI
const selectedFacets = reactive(
  Object.fromEntries(ACTIVE_SHOWCASE_FACETS.map(facet => [facet, [] as string[]]))
);



// 2. facetRefs for useSearch API (syncs with selectedFacets)
const facetRefs = Object.fromEntries(
  ACTIVE_SHOWCASE_FACETS.map(facet => [facet, computed({
    get: () => selectedFacets[facet],
    set: (val: string[]) => { selectedFacets[facet] = val }
  })])
);


// 3. Use selectedFacets everywhere in your code and UI
function resetAllFacets() {
  for (const key in selectedFacets){
    selectedFacets[key] = [];
  }
  // Reset the 'facets' query parameter
  const query = { ...route.query }
  if (query.page && query.page !== '1') {
    query.page = '1'; // Reset page to 1 if facets are restored from route
  }
  query['facets'] = encodeURIComponent(JSON.stringify({}))
  router.push({ query })
}

const onSearch = () => goToPage(1, { q: searchInput.value })

function goToPage(newPage: number | string, query = route.query) {
  const page = newPage ? Number(newPage) : 1
  // Collect all facet values from facetRefs
  const facetsQuery = ACTIVE_SHOWCASE_FACETS.reduce((acc, facet) => {
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

const piveauQueryParams: SearchParamsBase = reactive({
  limit: 10,
  page: route.query.page ? Number(route.query.page) - 1 : 0,
  q: Array.isArray(route.query.q) ? route.query.q.join(' ') : route.query.q || '',
  sort: 'relevance'
})


const { useSearch} = useShowcaseSearch()
const {
  query,
  getSearchResultsEnhanced,
  getAvailableFacetsLocalized,
  getSearchResultsCount

} = useSearch({
  queryParams: toRefs(piveauQueryParams),
  selectedFacets: facetRefs,
})

const availableFacets = getAvailableFacetsLocalized(locale.value);


const activeFacets = computed<SearchResultFacetGroupLocalized[]>(() => {
  const facets = availableFacets.value.filter(f => ACTIVE_SHOWCASE_FACETS.includes(f.id)).sort((a, b) => a.title.localeCompare(b.title))
  return facets
});

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
  ACTIVE_SHOWCASE_FACETS.forEach(facet => {
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
  } else {
   // syncFacetsFromRoute()
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

const { suspense } = query
await suspense()

</script>

<template>
  <OdsPage>
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
      <div  class="ods-card-list">
        <ul class="search-results-list">
          <li  v-for="showcase in getSearchResultsEnhanced" :key="showcase.id">
            <OdsCard
                style="height: 100%;"
                :title="getCurrentTranslation(showcase.title, locale.value)"
                clickable
              >
                <template #image>
                  <img :src="showcase.image[0]" :alt="getCurrentTranslation(showcase.title, locale.value)" >
                </template>

                <template #top-meta>
                  <div>
                    <span class="meta-info__item">{{ (showcase as any).type || 'fixme' }}</span>
                    <span class="meta-info__item">
                      {{ t('message.showcase.search.dataset_references', { count: showcase.references.length }) }}
                    </span>
                  </div>
                </template>

                <template #footer-info>
                  <div>
                    <span class="tag" v-for="tag in showcase.keywords" :key="tag.id">
                      {{ tag.label }}
                    </span>
                  </div>
                </template>

                <MDC :value="getCurrentTranslation(showcase.abstract, locale.value)" />

                <template #footer-action>
                  <NuxtLinkLocale :to="{ name: 'showcase-id', params: { id: showcase.id } }" type="false" class="btn btn--outline btn--icon-only" aria-label="false">
                    <SvgIcon icon="ArrowRight" role="btn" />
                    <span class="btn__text">Weiterlesen</span>
                  </NuxtLinkLocale>
                </template>
              </OdsCard>
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
