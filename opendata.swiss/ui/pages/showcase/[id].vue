<script setup>
import OdsPage from "../../app/components/OdsPage.vue"
import {homePageBreadcrumb} from "../../app/composables/breadcrumbs.js";
import OdsInfoBlock from "../../app/components/OdsInfoBlock.vue";
import OdsTagItem from "../../app/components/OdsTagItem.vue";
import OdsBreadcrumbs from "../../app/components/OdsBreadcrumbs.vue";
import OdsCard from "../../app/components/OdsCard.vue";
import OdsButton from "../../app/components/OdsButton.vue";
import {useDatasetsSearch, useVocabularySearch} from "../../app/piveau/search.js";

const route = useRoute()
const { locale, t } = useI18n()
const { id } = route.params

const { data: showcase } = await useAsyncData(route.path, () => {
  return queryCollection('showcases')
    .where('stem', 'LIKE', `%${id}.${locale.value}`)
    .where('active', '==', true)
    .first()
})

const breadcrumbs = [
  await homePageBreadcrumb(locale),
  {
    title: t('message.header.navigation.showcases'),
    path: '/showcases',
  },
  {
    title: showcase.value?.title || id,
  }
]

const showcaseCategories = await Promise.all(showcase.value?.categories.map(async categoryId => {
  const {query, resultEnhanced} = useVocabularySearch().useResource('data-theme/vocable', { additionalParams: { resource: categoryId }, })
  await query.suspense()
  return resultEnhanced.value
}))

const showcaseDatasets = await Promise.all(showcase.value.datasets.map(async ({id}) => {
  const {query, resultEnhanced} = useDatasetsSearch().useResource(id)
  await query.suspense()
  return resultEnhanced.value
}))

useSeoMeta({
  title: `${showcase.value?.title} | ${t('message.header.navigation.showcases')} | opendata.swiss`,
})
</script>

<template>
  <OdsPage v-if="showcase" :page="showcase" >
    <template #header>
      <OdsBreadcrumbs :breadcrumbs="breadcrumbs" />
    </template>

    <template #hero-subheading>
      <img v-if="showcase.image" :src="showcase.image" :alt="showcase.title" >
    </template>

    <template #aside-content>
      <OdsCard v-if="showcase.url" :title="t('message.showcase.externalLink')">
        <OdsButton icon="External" variant="outline-negative" :href="showcase.url">{{ t('message.showcase.open') }}</OdsButton>
      </OdsCard>

      <OdsCard :title="t('message.dataset_detail.additional_information')">
        <OdsInfoBlock :title="t('message.showcase.type.header')">
          {{ showcase.type }}
        </OdsInfoBlock>
        <OdsInfoBlock :title="t('message.showcase.categories')">
          <ul>
            <li v-for="category in showcaseCategories" :key="category.id">
              {{ category.pref_label }}
            </li>
          </ul>
        </OdsInfoBlock>
        <OdsInfoBlock :title="t('message.showcase.datasets')">
          <ul>
            <li v-for="dataset in showcaseDatasets" :key="dataset.getId">
              <NuxtLinkLocale :to="{ name: 'datasets-datasetId', params: { datasetId: dataset.getId } }">
                {{ dataset.getTitle }}
              </NuxtLinkLocale>
            </li>
          </ul>
        </OdsInfoBlock>
        <OdsInfoBlock :title="t('message.showcase.tags')">
          <OdsTagItem v-for="tag in showcase.tags" :key="tag" :label="tag" />
        </OdsInfoBlock>
        <OdsInfoBlock :title="t('message.showcase.submitted_by')">
          Pending
        </OdsInfoBlock>
      </OdsCard>
    </template>
  </OdsPage>
</template>
