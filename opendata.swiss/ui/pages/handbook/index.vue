<script lang="ts" setup>
import OdsBreadcrumbs from "../app/components/OdsBreadcrumbs.vue";
import { useBreadcrumbs } from "../app/composables/breadcrumbs";
import { loadPageBreadcrumb } from "../app/lib/breadcrumbs";
import OdsPage from "../app/components/OdsPage.vue";
import OdsSearchPanel from "../app/components/OdsSearchPanel.vue";
import {useRouter} from "vue-router";

const route = useRoute()
const { t, locale } = useI18n()

const breadcrumbs = await useBreadcrumbs({
  route,
  locale,
  loadContent: loadPageBreadcrumb(locale)
})

const {data: page} = await useAsyncData(route.path, () => {

  return queryCollection('pages')
    .where('path', 'LIKE', `%handbook.${locale.value}`)
    .first()
})

useSeoMeta({
  title: `${page.value?.title} | opendata.swiss`,
})

const searchInput = ref('');

const router = useRouter()
const onSearch = () => {
  if (searchInput.value.trim() !== '') {
    router.push({
      path: '/handbook/search',
      query: { q: searchInput.value.trim() },
    })
  }
};

</script>

<template>
  <OdsPage v-if="page" :page="page">
    <template #header>
      <OdsBreadcrumbs :breadcrumbs="breadcrumbs" />
    </template>
    <template #before-aside-content>
      <OdsSearchPanel
        aside
        :title="t('message.handbook.search_prompt')"
        :search-input="searchInput"
        :search-prompt="t('message.handbook.search_prompt_short')"
        @search="onSearch"
        @update:search-input="value => searchInput = value"
      />
    </template>
  </OdsPage>
</template>
