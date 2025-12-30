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
      />
    </template>
  </OdsPage>
</template>

<script setup lang="ts">
import OdsPage from '~/components/OdsPage.vue'
import OdsBreadcrumbs, { type BreadcrumbItem } from '~/components/OdsBreadcrumbs.vue'
import OdsSearchPanel from '~/components/OdsSearchPanel.vue'
import { useRouter } from '#vue-router'

const { t } = useI18n()

defineProps<{
  page: unknown
  breadcrumbs: BreadcrumbItem[]
}>()

const searchInput = ref('')

const router = useRouter()
const onSearch = (value: string) => {
  router.push({
    path: '/handbook/search',
    query: { q: value.trim() },
  })
}
</script>
