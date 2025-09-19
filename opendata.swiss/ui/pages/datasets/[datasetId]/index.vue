<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n';

import { useDatasetsSearch } from '../../../app/piveau/search'
import { DcatApChV2DatasetAdapter } from '../../../app/components/dataset-detail/model/dcat-ap-ch-v2-dataset-adapter';

import { homePageBreadcrumb } from "../../../app/composables/breadcrumbs";
import OdsBreadcrumbs from "../../../app/components/OdsBreadcrumbs.vue";
import OdsDetailTermsOfUse from '../../../app/components/dataset-detail/OdsDetailTermsOfUse.vue'
import OdsDetailsTable from '../../../app/components/dataset-detail/OdsDetailsTable.vue'
import OdsTagList from '../../../app/components/dataset-detail/OdsTagList.vue'
import OdsDatasetMetaInfo from '../../../app/components/dataset-detail/OdsDatasetMetaInfo.vue'
import OdsDistributionList from '../../../app/components/dataset-detail/OdsDistributionList.vue'
import OdsButton from '../../../app/components/OdsButton.vue';
import OdsDatasetCatalogPanel from '../../../app/components/dataset-detail/OdsDatasetCatalogPanel.vue'
import OdsMetadataDownloadList from '../../../app/components/dataset-detail/OdsMetadataDownloadList.vue'

import { useSeoMeta } from 'nuxt/app';

const { locale, t } = useI18n();
const route = useRoute()
const router = useRouter()
const datasetId = route.params.datasetId as string

const { useResource } = useDatasetsSearch()
const { query, isSuccess, resultEnhanced } = useResource(datasetId)

const { suspense } = query

const dataset = computed(() => {
  if (!resultEnhanced.value) {
    return undefined
  }
  return new DcatApChV2DatasetAdapter(resultEnhanced.value)
})

const distributions = computed(() => (dataset.value?.distributions ?? []).sort((a, b) => a.title.localeCompare(b.title)))


const homePage = await homePageBreadcrumb(locale)
const breadcrumbs = computed(() => {
  const result = [
    homePage,
    {
      title: t('message.header.navigation.datasets'),
      path: '/datasets',
    },
  ]

  if(route.query.search || typeof route.query.search === 'string') {
    const searchQuery = Array.isArray(route.query.search) ? route.query.search[0] : route.query.search;
    result.push({
      title: t('message.dataset_search.search_results'),
      route: {
        path: '/datasets',
        query: searchQuery
          ? Object.fromEntries(new URLSearchParams(decodeURIComponent(searchQuery)))
          : {}
      }
    })
  }

  result.push({
    title: resultEnhanced.value?.getTitle,
    path: {
      name: 'datasets-datasetId',
      params: { datasetId: datasetId },
    },
  })

  return result
})


useSeoMeta({
  title: () => `${dataset.value?.title} | ${t('message.header.navigation.datasets')} | opendata.swiss`
})


await suspense()

</script>

<template>
  <div v-if="isSuccess && dataset">
  <header id="main-header">
    <OdsBreadcrumbs :breadcrumbs="breadcrumbs" />
  </header>
  <main id="main-content">
   <section class="hero hero--default">
      <div class="container container--grid gap--responsive">
         <div class="hero__content">
            <OdsDatasetMetaInfo :dataset="dataset" />
            <h1 class="hero__title"> {{ dataset.title }} </h1>
            <MDC :value="dataset.description" />
            <!----><!---->
            <aside v-if="dataset.publisher" class="authors">
              <div class="disc-images" aria-hidden="true">
                <div class="disc-image"><img src="https://picsum.photos/120/120/?image=29" :title="dataset.publisher.name"></div>
              </div>
              <address class="authors__names">
                <a class="link author__name link--external" target="_blank" :href="dataset.publisher.resource">{{ dataset.publisher.name }}</a>
              </address>
            </aside>
         </div>
         <!---->
      </div>
   </section>
   <section class="section">
      <div class="container container--grid gap--responsive">
         <div class="container__main vertical-spacing">
            <div class="container__mobile">
               <div class="box">
                  <h2 class="h5">{{ t(`message.header.navigation.terms_of_use`) }}</h2>
                  <OdsDetailTermsOfUse v-for="value in dataset.licenses" :key="value" :name="value" />
               </div>
            </div>
            <h2 class="h2">{{ t('message.dataset_detail.distributions') }}</h2>
            <OdsDistributionList :distributions="distributions" />

            <h2 class="h2">{{ t('message.dataset_detail.additional_information') }}</h2>
            <OdsDetailsTable :table-entries="dataset.propertyTable"/>
            <div>
               <h2 class="h2">{{ t('message.dataset_detail.categories') }}</h2>
               <div>
                  <OdsTagList :tags="resultEnhanced?.getCategories ?? []" />
               </div>
            </div>
            <div v-if="dataset.catalog" >
              <h2 class="h2">{{ t('message.dataset_detail.catalog') }}</h2>
              <OdsDatasetCatalogPanel :dataset="dataset" />
            </div>
         </div>
         <div class="hidden container__aside md:block">
            <div id="aside-content" class="sticky sticky--top">
               <div class="box">
                  <h2 class="h5">{{ t(`message.header.navigation.terms_of_use`) }}</h2>
                  <OdsDetailTermsOfUse v-for="value in resultEnhanced?.getLicenses" :key="value" :name="value" />
               </div>
                 <div class="box">
                  <h2 class="h5">{{ t(`message.dataset_detail.metadata_download`) }}</h2>
                  <OdsMetadataDownloadList :dataset="dataset" />
               </div>
            </div>
         </div>
      </div>
   </section>

   <section class="section publication-back-button-section">
      <div class="container">
        <OdsButton title="Zurück" icon="ArrowLeft" variant="outline" class="btn--back" size="sm" @click="router.back()" />
      </div>
   </section>
  </main>
</div>

</template>

<style lang="scss" scoped>

</style>
