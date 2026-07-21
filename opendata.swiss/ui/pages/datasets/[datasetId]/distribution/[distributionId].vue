<script setup lang="ts">
import { useI18n } from '#imports'

import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useDatasetsSearch } from '../../../../app/piveau/datasets'
import { homePageBreadcrumb } from '../../../../app/composables/breadcrumbs.js'
import OdsDetailsTable from '../../../../app/components/dataset-detail/OdsDetailsTable.vue'
import OdsBreadcrumbs from '../../../../app/components/OdsBreadcrumbs.vue'
import OdsButton from '../../../../app/components/OdsButton.vue'
import OdsDownloadList from '../../../../app/components/distribution/OdsDownloadList.vue'
import OdsDistributionDetailHeader from '../../../../app/components/distribution/OdsDistributionDetailHeader.vue'
import OdsHero from '../../../../app/components/OdsHero.vue'
import { DcatApChV2DatasetAdapter } from '../../../../app/components/dataset-detail/model/dcat-ap-ch-v2-dataset-adapter'
import { useSeoMeta } from 'nuxt/app'
import { getDatasetBreadcrumbFromSessionStorage } from '../breadcrumb-session-stoage'
import SvgIcon from '../../../../app/components/SvgIcon.vue'

const { locale, t } = useI18n()

const localePath = useLocalePath()

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

const hasDownloadUrl = computed(() => {
  const dist = distribution.value

  if (!dist) {
    // we are not ready
    return false
  }
  const downloadUrls = dist.downloadUrls
  if (downloadUrls.length > 0) {
    // we have at least one download url
    return true
  }
  // empty array return false
  return false
})

const hasAccessUrl = computed(() => {
  const dist = distribution.value

  if (!dist) {
    // we are not ready
    return false
  }
  const accessUrls = dist.accessUrls
  if (accessUrls.length > 0) {
    // we have at least one accessUrl url
    return true
  }
  // empty array return false
  return false
})

const firstBreadcrumb = await homePageBreadcrumb(locale)

const breadcrumbs = computed(() => {
  const bc = []
  const storedBreadcrumbs = import.meta.client ? getDatasetBreadcrumbFromSessionStorage(datasetId) : null
  if (storedBreadcrumbs && import.meta.client) {
    if (storedBreadcrumbs.length === 4) {
      storedBreadcrumbs[1].title = t('message.header.navigation.datasets')
      storedBreadcrumbs[2].title = t('message.dataset_search.search_results')
      storedBreadcrumbs[3].title = dataset.value?.title ?? ''
    }
    else if (storedBreadcrumbs.length === 3) {
      storedBreadcrumbs[1].title = t('message.header.navigation.datasets')
      storedBreadcrumbs[2].title = dataset.value?.title ?? ''
    }
    bc.push(...storedBreadcrumbs)
    bc.push({
      title: distribution.value?.title,
    })
  }
  else {
    bc.push(firstBreadcrumb)
    bc.push({
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
      title: distribution.value?.title,
    },
    )
  }
  return bc
})

useSeoMeta({
  title: () => `${distribution.value?.title} | ${dataset.value?.title} | ${t('message.header.navigation.datasets')} | opendata.swiss`,
})

const toDatasetHref = computed(() => localePath('/datasets/' + datasetId))

function gotToDataset() {
  router.push(toDatasetHref.value)
}
await suspense()
</script>

<template>
  <main
    v-if="isSuccess && distribution"
    id="main-content"
  >
    <header id="main-header">
      <ClientOnly>
        <OdsBreadcrumbs :breadcrumbs="breadcrumbs" />
      </ClientOnly>
    </header>
    <section class="section section--default bg--secondary-50">
      <div class="container">
        <div style="display: flex; flex-direction: row; justify-content: space-between; margin-bottom: 32px;">
          <span class="dataset-label">{{ t('message.dataset_detail.distribution') }}</span>
          <a
            v-if="hasDownloadUrl"
            class="big-button"
            target="_blank"
          >
            <SvgIcon
              icon="Download"
              size="xl"
            />
            <span>{{ t('message.dataset_detail.download') }} {{ distribution.format }}</span>
          </a>
          <a
            v-if="hasAccessUrl && !hasDownloadUrl"
            :href="distribution.accessUrls[0]"
            target="_blank"
            class="big-button"
          >
            <SvgIcon
              icon="External"
              size="xl"
            />
            <span>Go To {{ distribution.format }}</span>
          </a>
        </div>
        <OdsDistributionDetailHeader :distribution="distribution" />
      </div>
    </section>

    <OdsHero
      type="default"
      floating
    >
      <template #title>
        {{ distribution.title }}
      </template>
      <template #description>
        <MDC :value="distribution.description ?? ''" />
      </template>
    </OdsHero>
    <section class="section">
      <div class="container container--grid gap--responsive">
        <div class="container__main vertical-spacing">
          <div class="container__mobile">
            <div
              v-if="hasDownloadUrl"
              class="box"
            >
              <h2 class="h5">
                {{ t('message.dataset_detail.download') }}
              </h2>
              <OdsDownloadList
                :urls="distribution.downloadUrls"
                :name="distribution.title"
                :format="distribution.format"
                :languages="distribution.languages"
                :byte-size="distribution.formattedByteSize"
                icon="Download"
              />
            </div>
            <div
              v-if="hasAccessUrl"
              class="box"
            >
              <h2 class="h5">
                Access
              </h2>
              <OdsDownloadList
                :urls="distribution.accessUrls"
                :name="distribution.title"
                :format="distribution.format"
                :languages="distribution.languages"
                :byte-size="distribution.formattedByteSize"
                icon="External"
              />
            </div>
          </div>
          <h2 class="h2">
            {{ t('message.dataset_detail.additional_information') }}
          </h2>
          <OdsDetailsTable
            :table-entries="distribution.propertyTable"
            type="block"
          />
        </div>
        <div class="hidden container__aside md:block">
          <div
            id="aside-content"
            class="sticky sticky--top"
          >
            <div
              v-if="distribution.downloadUrls.length > 0"
              class="box"
            >
              <h2 class="h5">
                {{ t('message.dataset_detail.download') }}
              </h2>
              <OdsDownloadList
                :urls="distribution.downloadUrls"
                :name="distribution.title"
                :format="distribution.format"
                :languages="distribution.languages"
                :byte-size="distribution.formattedByteSize"
                icon="Download"
              />
            </div>
            <div
              v-if="hasAccessUrl && !hasDownloadUrl"
              class="box"
            >
              <h2 class="h5">
                Access
              </h2>
              <OdsDownloadList
                :urls="distribution.accessUrls"
                :name="distribution.title"
                :format="distribution.format"
                :languages="distribution.languages"
                :byte-size="distribution.formattedByteSize"
                icon="External"
              />
            </div>
          </div>
        </div>
      </div>
    </section>
    <section class="section publication-back-button-section">
      <div class="container">
        <OdsButton
          :title="t(`message.dataset_detail.to_dataset`) "
          icon="ArrowLeft"
          variant="outline"
          class="btn--back"
          @click="gotToDataset()"
        />
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

#main-header {
   @media (min-width: 1024px) {
    min-height: 65.5px;
  }
  @media (min-width: 1280px) {
    min-height: 73.5px;
  }
}

.dataset-label {
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
  height: fit-content;
}

.big-button {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 5px;
  background-color: var(--color-primary-600);
  color: white;
  padding: 12px;
  padding-right: 24px;
  text-decoration: none;
}
</style>
