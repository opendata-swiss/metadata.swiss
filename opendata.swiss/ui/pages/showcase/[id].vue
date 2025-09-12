<script setup>
import OdsPage from "../../app/components/OdsPage.vue"
import {homePageBreadcrumb} from "../../app/composables/breadcrumbs.js";
import OdsInfoBlock from "../../app/components/OdsInfoBlock.vue";
import OdsTagItem from "../../app/components/OdsTagItem.vue";
import OdsBreadcrumbs from "../../app/components/OdsBreadcrumbs.vue";
import OdsCard from "../../app/components/OdsCard.vue";
import OdsButton from "../../app/components/OdsButton.vue";

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
            <li v-for="category in showcase.categories" :key="category">
              {{ category }}
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
