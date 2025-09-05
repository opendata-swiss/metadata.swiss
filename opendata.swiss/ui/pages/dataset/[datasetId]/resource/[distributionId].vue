<script setup lang="ts">
import { definePropertyNode, } from '@piveau/sdk-vue'
import { useI18n } from 'vue-i18n'

import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useDatasetsSearch } from '../../../../app/piveau/search.js'
import { homePageBreadcrumb } from "../../../../app/composables/breadcrumbs.js";
import OdsDetailTermsOfUse from '../../../../app/components/dataset-detail/OdsDetailTermsOfUse.vue';
import OdsDetailsTable from '../../../../app/components/dataset-detail/OdsDetailsTable.vue'
const { locale, t } = useI18n();

const route = useRoute()
const router = useRouter()
const datasetId = computed(() => route.params.datasetId as string)
const distributionId = computed(() => route.params.distributionId as string)


const { useResource } = useDatasetsSearch()
const { isSuccess, resultEnhanced } = useResource(datasetId)

const distribution = computed(() => {
  const dists = resultEnhanced.value?.getDistributions.find(d => d.id === distributionId.value) ?? undefined
  return dists
})

const node = computed(() => {
  if (!distribution.value) {
    return null
  }
  const rootNode = definePropertyNode({ id: 'root', data: distribution.value.getPropertyTable}, { compact: true, maxDepth: 2 })
  return rootNode
})

const _breadcrumbs = [
  await homePageBreadcrumb(locale),
  {
    title: t('message.header.navigation.datasets'),
    path: '/datasets',
  },
  {
    title: resultEnhanced.value?.getTitle,
    path: route.path,
  }
]
</script>

<template>
  <main v-if="isSuccess && distribution" id="main-content">
    <section class="hero hero--default">
      <div class="container container--grid gap--responsive">
        <div class="hero__content">
          <p class="meta-info"><span class="meta-info__item">{{ t('message.dataset_detail.distribution') }}</span><span class="meta-info__item">{{ t('message.dataset_detail.published_on') }} {{ resultEnhanced?.getCreated }} </span><span class="meta-info__item">{{ t('message.dataset_detail.modified_on') }} {{ resultEnhanced?.getModified }} </span></p>
          <h1 class="hero__title"> {{ distribution.title }} </h1>
          <h2 class="hero__subtitle"> {{ distribution.description }} </h2>
        </div>
      </div>
   </section>
  <section class="section">
    <div class="container container--grid gap--responsive">
      <div class="container__main vertical-spacing">
        <div class="container__mobile">
          <div class="box">
            <h2 class="h5">Download</h2>
              <ul class="download-items">
                <li>
                  <a v-for="link in distribution.downloadUrls" :key="link" :href="link"  class="download-item"  target="_blank">
                    <svg viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg" class="icon icon--xl icon--Download download-item__icon">
                      <path xmlns="http://www.w3.org/2000/svg" d="m19.419 13.698-.375-.649-6.294 3.634v-12.228h-.75v12.228l-6.294-3.634-.375.649 7.044 4.067z" />
                      <path xmlns="http://www.w3.org/2000/svg" d="m6.00576 19.91649h12.76855v.75h-12.76855z"/>
                    </svg>
                    <div>
                      <h2 class="download-item__title">{{ distribution.title }}</h2>
                      <p class="meta-info download-item__meta-info"><span class="meta-info__item">{{ distribution.format ? distribution.format : distribution.title }}</span><span class="meta-info__item">{{ distribution.languages.join(', ') }}</span></p>
                    </div>
                  </a>
                </li>
              </ul>
            </div>
            <div class="box">
              <h2 class="h5">{{ t(`message.header.navigation.terms_of_use`) }}</h2>
              <OdsDetailTermsOfUse v-for="value in resultEnhanced?.getLicenses" :key="value" :name="value" />
            </div>
          </div>
          <h2 class="h2">{{ t('message.dataset_detail.additional_information') }}</h2>
          <OdsDetailsTable v-if="node" :root-node="node"/>
        </div>
        <div class="hidden container__aside md:block">
          <div id="aside-content" class="sticky sticky--top">
            <div class="box">
              <h2 class="h5">Download</h2>
              <ul class="download-items">
                <li>
                  <a v-for="link in distribution.downloadUrls" :key="link" :href="link"  class="download-item"  target="_blank">
                    <svg viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg" class="icon icon--xl icon--Download download-item__icon">
                      <path xmlns="http://www.w3.org/2000/svg" d="m19.419 13.698-.375-.649-6.294 3.634v-12.228h-.75v12.228l-6.294-3.634-.375.649 7.044 4.067z" />
                      <path xmlns="http://www.w3.org/2000/svg" d="m6.00576 19.91649h12.76855v.75h-12.76855z"/>
                    </svg>
                    <div>
                      <h2 class="download-item__title">{{ distribution.title }}</h2>
                      <p class="meta-info download-item__meta-info"><span class="meta-info__item">{{ distribution.format ? distribution.format : distribution.title }}</span><span class="meta-info__item">{{ distribution.languages.join(', ') }}</span></p>
                    </div>
                  </a>
                </li>
              </ul>
            </div>
            <div class="box">
              <h2 class="h5">{{ t(`message.header.navigation.terms_of_use`) }}</h2>
              <OdsDetailTermsOfUse v-for="value in resultEnhanced?.getLicenses" :key="value" :name="value" />
            </div>
          </div>
        </div>
      </div>
    </section>
    <section class="section publication-back-button-section">
      <div class="container">
        <OdsButton title="Zurück" icon="ArrowLeft" @click="router.back()" class="btn--back"></OdsButton>
      </div>
    </section>
    <pre>{{ distribution }}</pre>
  </main>
</template>

<style lang="scss" scoped>

</style>
