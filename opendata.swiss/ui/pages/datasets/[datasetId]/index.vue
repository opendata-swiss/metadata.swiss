<script setup lang="ts">
import { definePropertyNode } from '@piveau/sdk-vue'
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useDatasetsSearch } from '../../../app/piveau/search'
import { homePageBreadcrumb } from "../../../app/composables/breadcrumbs";
import OdsBreadcrumbs from "../../../app/components/OdsBreadcrumbs.vue";
import OdsDetailTermsOfUse from '../../../app/components/dataset-detail/OdsDetailTermsOfUse.vue'
import OdsDetailsTable from '../../../app/components/dataset-detail/OdsDetailsTable.vue'
import OdsTagList from '../../../app/components/dataset-detail/OdsTagList.vue'
import OdsDownloadsList from '../../../app/components/dataset-detail/OdsDownloadsList.vue'
import OdsDatasetMetaInfo from '../../../app/components/dataset-detail/OdsDatasetMetaInfo.vue'

import { useI18n } from 'vue-i18n';
const md = `
::alert
Hello MDC
::
`
const { locale, t } = useI18n();
const route = useRoute()
const router = useRouter()
const datasetId = computed(() => route.params.datasetId as string)

const { useResource } = useDatasetsSearch()
const { isSuccess, resultEnhanced } = useResource(datasetId)

const node = computed(() => definePropertyNode({ id: 'root', data: resultEnhanced.value?.getPropertyTable }, { compact: true, maxDepth: 2 }))

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
    result.push({
      title: t('message.dataset_search.search_results'),
      route: {
        path: '/datasets',
        query: Object.fromEntries(new URLSearchParams(decodeURIComponent(route.query.search)))
      }
    })
  }

  result.push({
    title: resultEnhanced.value?.getTitle,
    path: {
      name: 'datasets-datasetId',
      params: { datasetId: datasetId.value },
    },
  })

  return result
})
</script>

<template>
  <div v-if="isSuccess">
  <header id="main-header">
    <OdsBreadcrumbs :breadcrumbs="breadcrumbs" />
  </header>
  <main id="main-content">
   <section class="hero hero--default">
      <div class="container container--grid gap--responsive">
         <div class="hero__content">
            <OdsDatasetMetaInfo :dataset="resultEnhanced" />
            <h1 class="hero__title"> {{ resultEnhanced?.getTitle }} </h1>
            <h2 class="hero__subtitle"> {{ resultEnhanced?.getDescription }} </h2>
            <pre>{{ resultEnhanced?.getDescription }}</pre>
            <!----><!---->
            <aside class="authors">
               <div class="disc-images" aria-hidden="true">
                  <div class="disc-image"><img src="https://picsum.photos/120/120/?image=29" :title="resultEnhanced?.getPublisher?.name ?? ''"></div>
               </div>
               <address class="authors__names">
                <a class="link author__name" href="#">{{ resultEnhanced?.getPublisher?.name }}</a>
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
                  <h2 class="h5">Download</h2>
                  <OdsDownloadsList :dataset="resultEnhanced" />
               </div>
               <div class="box">
                  <h2 class="h5">{{ t(`message.header.navigation.terms_of_use`) }}</h2>
                  <OdsDetailTermsOfUse v-for="value in resultEnhanced?.getLicenses" :key="value" :name="value" />
               </div>
            </div>
            <h2 class="h2">{{ t('message.dataset_detail.additional_information') }}</h2>
            <OdsDetailsTable :root-node="node"/>
            <div>
               <h2 class="h2">{{ t('message.dataset_detail.categories') }}</h2>
               <div>
                  <OdsTagList :tags="resultEnhanced?.getCategories ?? []" />
               </div>
            </div>
         </div>
         <div class="hidden container__aside md:block">
            <div id="aside-content" class="sticky sticky--top">
               <div class="box">
                  <h2 class="h5">{{ t('message.dataset_detail.go_to_resource') }}</h2>
                  <OdsDownloadsList :dataset="resultEnhanced"/>
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
        <OdsButton title="Zurück" icon="ArrowLeft" @click="router.back()" variant="outline" class="btn--back" size="sm"></OdsButton>
      </div>
   </section>
   <section class="section bg--secondary-50">
      <div class="container">
         <h2 class="section__title">Das könnte Sie auch interessieren</h2>
         <div class="grid grid--responsive-cols-3 gap--responsive">
            <div class="card card--universal card--clickable">
               <!----><!---->
               <div class="card__content">
                  <div class="card__body">
                     <p class="meta-info"><span class="meta-info__item">Bericht</span><span class="meta-info__item">03. März 2000</span></p>
                     <div class="card__title">
                        <h3>Auswirkungen von Corona auf die Schweizer Gesellschaft</h3>
                     </div>
                     <p> Sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. </p>
                     <div class="card__image"><img src="https://swiss.github.io/designsystem/images/publication-cover.png" alt="cat"></div>
                     <p class="meta-info"><span class="meta-info__item">PDF</span><span class="meta-info__item">3.8 Mb</span><span class="meta-info__item">102 Seiten</span><span class="meta-info__item">Deutsch</span></p>
                  </div>
                  <div class="card__footer card__footer--icon-only">
                     <!---->
                     <div class="card__footer__action">
                       <OdsButton
                         variant="outline"
                         icon-only
                         icon="ArrowRight"
                         title="Weiterlesen"></OdsButton>
                     </div>
                  </div>
               </div>
            </div>
            <div class="card card--universal card--clickable">
               <!----><!---->
               <div class="card__content">
                  <div class="card__body">
                     <p class="meta-info"><span class="meta-info__item">Bericht</span><span class="meta-info__item">03. März 2000</span></p>
                     <div class="card__title">
                        <h3>Auswirkungen von Corona auf die Schweizer Gesellschaft</h3>
                     </div>
                     <p> Sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. </p>
                     <div class="card__image"><img src="https://swiss.github.io/designsystem/images/publication-cover.png" alt="cat"></div>
                     <p class="meta-info"><span class="meta-info__item">PDF</span><span class="meta-info__item">3.8 Mb</span><span class="meta-info__item">102 Seiten</span><span class="meta-info__item">Deutsch</span></p>
                  </div>
                  <div class="card__footer card__footer--icon-only">
                     <!---->
                     <div class="card__footer__action">
                       <OdsButton
                         variant="outline"
                         icon-only
                         @click="router.back()"
                         icon="ArrowRight"
                         title="Weiterlesen"></OdsButton>
                     </div>
                  </div>
               </div>
            </div>
         </div>
      </div>
   </section>
   <pre>{{ resultEnhanced }}</pre>
  </main>
</div>

</template>

<style lang="scss" scoped>

</style>
