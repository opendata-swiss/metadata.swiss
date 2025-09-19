<script setup lang="ts">
import { useI18n } from 'vue-i18n'

import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useDatasetsSearch } from '../../../../app/piveau/search.js'
import { homePageBreadcrumb } from "../../../../app/composables/breadcrumbs.js";
import OdsDetailTermsOfUse from '../../../../app/components/dataset-detail/OdsDetailTermsOfUse.vue';
import OdsDetailsTable from '../../../../app/components/dataset-detail/OdsDetailsTable.vue'
import OdsBreadcrumbs from "../../../../app/components/OdsBreadcrumbs.vue";
import OdsButton from "../../../../app/components/OdsButton.vue";
import OdsDownloadList from '../../../../app/components/distribution/OdsDownloadList.vue'
import OdsRelativeDateToggle from '../../../../app/components/OdsRelativeDateToggle.vue';
import { DcatApChV2DatasetAdapter } from '../../../../app/components/dataset-detail/model/dcat-ap-ch-v2-dataset-adapter.js'
import { useSeoMeta } from 'nuxt/app';


const { locale, t } = useI18n();

const route = useRoute()
const router = useRouter()
const datasetId = route.params.datasetId as string
const distributionId = route.params.distributionId as string


const { useResource } = useDatasetsSearch()
const { query, isSuccess, resultEnhanced } = useResource(datasetId)

const { suspense } = query

const dataset = computed(() => {
  if (!resultEnhanced.value) {
    return undefined
  }
  return new DcatApChV2DatasetAdapter(resultEnhanced.value)
})


const distribution = computed(() => {
  if (!dataset.value) {
    return undefined
  }
  const dists = dataset.value.distributions.find(d => d.id === distributionId) ?? undefined
  return dists
})


const breadcrumbs = [
  await homePageBreadcrumb(locale),
  {
    title: t('message.header.navigation.datasets'),
    path: '/datasets',
  },
  {
    title: resultEnhanced.value?.getTitle,
    path: {
      name: 'datasets-datasetId',
      params: { datasetId: datasetId },
    },
  },
  {
    title: distribution.value?.title
  }
]

useSeoMeta({
  title: () => `${distribution.value?.title} | ${dataset.value?.title} | ${t('message.header.navigation.datasets')} | opendata.swiss`,
})

await suspense()
</script>

<template>
  <main v-if="isSuccess && distribution" id="main-content">
    <header id="main-header">
      <OdsBreadcrumbs :breadcrumbs="breadcrumbs" />
    </header>
    <section class="hero hero--default">
      <div class="container container--grid gap--responsive">
        <div class="hero__content">
          <span class="distribution-label">{{ t('message.dataset_detail.distribution') }}</span>

          <p class="meta-info">
            <span v-if="distribution.releaseDate" class="meta-info__item">
              {{ t('message.dataset_detail.published_on') }}
               <OdsRelativeDateToggle :date="distribution.releaseDate" />
            </span>
            <span v-if="distribution.modified" class="meta-info__item">{{ t('message.dataset_detail.modified_on') }}
              <OdsRelativeDateToggle :date="distribution.modified" />
            </span>
          </p>
          <h1 class="hero__title"> {{ distribution.title }} </h1>
          <MDC :value="distribution.description ?? ''" />
        </div>
      </div>
    </section>
    <section class="section">
      <div class="container container--grid gap--responsive">
        <div class="container__main vertical-spacing">
          <div class="container__mobile">
            <div v-if="distribution.downloadUrls.length > 0" class="box">
              <h2 class="h5">Download</h2>
                <OdsDownloadList :download-urls="distribution.downloadUrls" :name="distribution.title" :format="distribution.format" :languages="distribution.languages" :byte-size="distribution.formattedByteSize"/>
            </div>
            <div class="box">
              <h2 class="h5">Access</h2>
              <OdsDownloadList :download-urls="distribution.accessUrls" :name="distribution.title" :format="distribution.format" :languages="distribution.languages" :byte-size="distribution.formattedByteSize"/>
            </div>
            <div class="box">
              <h2 class="h5">{{ t(`message.header.navigation.terms_of_use`) }}</h2>
              <OdsDetailTermsOfUse v-if="distribution.license" :name="distribution.license" />
            </div>
          </div>
          <h2 class="h2">{{ t('message.dataset_detail.additional_information') }}</h2>
          <OdsDetailsTable :table-entries="distribution.propertyTable"/>
        </div>
        <div class="hidden container__aside md:block">
          <div id="aside-content" class="sticky sticky--top">
            <div v-if="distribution.downloadUrls.length > 0" class="box">
              <h2 class="h5">Download</h2>
              <OdsDownloadList :download-urls="distribution.downloadUrls" :name="distribution.title" :format="distribution.format" :languages="distribution.languages" :byte-size="distribution.formattedByteSize"/>
            </div>
             <div class="box">
              <h2 class="h5">Access</h2>
              <OdsDownloadList :download-urls="distribution.accessUrls" :name="distribution.title" :format="distribution.format" :languages="distribution.languages" :byte-size="distribution.formattedByteSize"/>
            </div>
            <div class="box">
              <h2 class="h5">{{ t(`message.header.navigation.terms_of_use`) }}</h2>
              <OdsDetailTermsOfUse v-if="distribution.license" :name="distribution.license" />
            </div>
          </div>
        </div>
      </div>
    </section>
    <section class="section publication-back-button-section">
      <div class="container">
        <OdsButton title="Zurück" icon="ArrowLeft" variant="outline" class="btn--back" @click="router.back()" />
      </div>
    </section>
  </main>
</template>

<style lang="scss" scoped>
.distribution-label {
  position: relative;
  background-color: #e6f0fa;
  color: #1976d2;
  padding: 2px 10px;
  border-radius: 6px;
  font-weight: 600;
  letter-spacing: 0.03em;
  display: inline-block;
  margin-right: 10px;
  vertical-align: middle;
  border: 1px solid #b3d4fc;
}
</style>
