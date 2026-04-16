<script setup lang="ts">
import { homePageBreadcrumb } from '../../app/composables/breadcrumbs.js'
import { useAsyncData, useSeoMeta } from 'nuxt/app'
import OdsCmsSearchPage from '../../app/components/OdsCmsSearchPage.vue'
import { useGetArticleUrl } from '../../app/composables/handbook.js'
import type { SearchResult } from 'minisearch'

const { t, locale } = useI18n()

const getArticleUrl = await useGetArticleUrl()

const { data: pages } = await useAsyncData('handbook-search-pages', async () => {
  return queryCollection('handbook')
    .select('path', 'title', 'parent', 'slug')
    .where('path', 'LIKE', `%.${locale.value}%`)
    .all()
})

const currentLanguage = new RegExp(`\\.${locale.value}(#.+)?$`)
const { data, error } = await useAsyncData('blog-search', async () => {
  const sections = await queryCollectionSearchSections('blog')
  return (sections || [])
    .filter(section => currentLanguage.test(section.id))
})

const searchResultToLink = (pages: BlogCollectionItem[]) => (result: SearchResult) => {
  const [id, hash] = result.id.split('#')

  let path
  const page = pages.find(({ path }) => result.id.startsWith(path))
  if (page) {
    path = getArticleUrl(page)
  }
  else {
    path = id.substring(0, id.lastIndexOf('.'))
  }
  return {
    path,
    hash: hash ? `#${hash}` : null,
  }
}

if (error.value) {
  console.error('Failed to fetch blog search sections', error.value)
}

const breadcrumbs = [
  await homePageBreadcrumb(locale),
  {
    title: t('message.header.navigation.blog'),
    path: '/blog',
  },
  {
    title: t('message.header.navigation.search'),
  },
]

useSeoMeta({
  title: `${t('message.header.navigation.search')} | ${t('message.header.navigation.blog')} | opendata.swiss`,
})
</script>

<template>
  <OdsCmsSearchPage
    :search-prompt="t('message.blog.search_prompt')"
    :breadcrumbs="breadcrumbs"
    :data="data"
    :search-result-to-link="searchResultToLink(pages)"
  />
</template>
