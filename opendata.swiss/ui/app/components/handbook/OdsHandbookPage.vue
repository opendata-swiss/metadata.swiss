<template>
  <OdsPage
    v-if="page"
    :page="page"
  >
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
      <OdsCard title="Handbook">
        <OdsAccordion id="handbook">
          <OdsAccordionItem
            v-for="section in sections"
            :id="section.id"
            :key="section.id"
            :title="section.title"
            :open="isSectionOpen(section)"
          >
            <NuxtLinkLocale
              v-for="article in getArticlesBySection(section)"
              :key="article.id"
              :to="`/handbook/${section.title.toLowerCase()}/${article.permalink}`"
              :class="['menu__item', 'menu__item--border', 'menu__item--condensed', { 'menu__item--active': page.id === article.id }]"
              style="margin-top: unset"
            >
              <div>{{ article.title }}</div>
            </NuxtLinkLocale>
          </OdsAccordionItem>
        </OdsAccordion>
      </OdsCard>
    </template>
  </OdsPage>
</template>

<script setup lang="ts">
import type { HandbookCollectionItem, HandbookSectionsCollectionItem, PagesCollectionItem } from '@nuxt/content'
import OdsPage from '~/components/OdsPage.vue'
import OdsBreadcrumbs, { type BreadcrumbItem } from '~/components/OdsBreadcrumbs.vue'
import OdsSearchPanel from '~/components/OdsSearchPanel.vue'
import { useRouter } from '#vue-router'
import OdsCard from '~/components/OdsCard.vue'
import OdsAccordion from '~/components/OdsAccordion.vue'
import OdsAccordionItem from '~/components/OdsAccordionItem.vue'

const { t, locale } = useI18n()

const { page } = defineProps<{
  page: HandbookCollectionItem | HandbookSectionsCollectionItem | PagesCollectionItem
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

const { data: sections } = await useAsyncData('handbook-sections', () =>
  queryCollection('handbookSections')
    .where('path', 'LIKE', `%.${locale.value}`)
    .all(),
)

const { data: articles } = await useAsyncData('handbook-articles', () =>
  queryCollection('handbook')
    .where('path', 'LIKE', `%.${locale.value}`)
    // .where('parent', 'IS NULL')
    .all(),
)

function getArticlesBySection(section: HandbookSectionsCollectionItem) {
  return articles.value
    ?.filter(article => article.section.toLowerCase() === section.title.toLowerCase())
    ?.sort((a, b) => a.order || Number.MAX_SAFE_INTEGER - (b.order || Number.MAX_SAFE_INTEGER)) || []
}

function isSectionOpen(section: HandbookSectionsCollectionItem) {
  if ('section' in page) {
    return page.section.toLowerCase() === section.title.toLowerCase()
  }

  if (page.stem.startsWith('pages')) {
    return true
  }

  return page.id === section.id
}
</script>
