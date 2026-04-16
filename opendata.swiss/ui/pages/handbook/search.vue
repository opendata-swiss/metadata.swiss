<script setup>
import { useRouter } from 'vue-router'
import MiniSearch from 'minisearch'
import { useGetArticleUrl } from '../../app/composables/handbook.js'
import OdsBreadcrumbs from '../../app/components/OdsBreadcrumbs.vue'
import OdsPage from '../../app/components/OdsPage.vue'
import OdsSearchPanel from '../../app/components/OdsSearchPanel.vue'
import { homePageBreadcrumb } from '../../app/composables/breadcrumbs.js'
import OdsCard from '../../app/components/OdsCard.vue'
import OdsSearchResults from '../../app/components/OdsSearchResults.vue'
import { useSeoMeta } from 'nuxt/app'

const { t, locale } = useI18n()
const route = useRoute()
const router = useRouter()

const searchInput = ref(route.query.q)
const getArticleUrl = await useGetArticleUrl()

const onSearch = (value) => {
  searchInput.value = value
  router.push({
    name: route.name,
    query: { q: value?.trim() || undefined },
  })
}

const { data: pages } = await useAsyncData('handbook-search-pages', async () => {
  return queryCollection('handbook')
    .select('path', 'title', 'parent', 'slug')
    .where('path', 'like', `%.${locale.value}%`)
    .all()
})

const currentLanguage = new RegExp(`\\.${locale.value}(#.+)?$`)
const { data, error } = await useAsyncData('handbook-search', async () => {
  const sections = await queryCollectionSearchSections('handbook')

  return (sections || [])
    .filter(section => currentLanguage.test(section.id))
})

function searchResultToLink(result, pages) {
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
  console.error('Failed to fetch handbook search sections', error.value)
}

const miniSearch = computed(() => {
  const ms = new MiniSearch({
    fields: ['title', 'titles', 'content'],
    storeFields: ['title', 'titles', 'content'],
    searchOptions: {
      prefix: true,
      fuzzy: 0.2,
    },
  })

  ms.addAll(toValue(data.value))

  return ms
})

const result = computed(() => {
  if (!miniSearch.value) return []

  return miniSearch.value.search(toValue(searchInput) || '')
    .sort((a, b) => a.score > b.score ? -1 : a.score < b.score ? 1 : 0)
    .slice(0, 10)
})

const breadcrumbs = [
  await homePageBreadcrumb(locale),
  {
    title: t('message.header.navigation.handbook'),
    path: '/handbook',
  },
  {
    title: t('message.header.navigation.search'),
  },
]

watch(
  () => route.query.q,
  (value) => {
    searchInput.value = value
  },
)

useSeoMeta({
  title: `${t('message.header.navigation.search')} | ${t('message.header.navigation.handbook')} | opendata.swiss`,
})
</script>

<template>
  <OdsPage>
    <template #header>
      <OdsBreadcrumbs :breadcrumbs="breadcrumbs" />
      <OdsSearchPanel
        v-model:search-input="searchInput"
        :search-prompt="t('message.handbook.search_prompt')"
        @search="onSearch"
      />
    </template>
    <OdsSearchResults :results-count="result.length">
      <OdsCard
        v-for="article in result"
        :key="article.id"
        :title="article.title"
        type="list"
        clickable
      >
        <p>{{ article.content }}</p>

        <template #footer-action>
          <NuxtLinkLocale
            :to="searchResultToLink(article, pages)"
            class="btn btn--outline btn--icon-only"
            aria-label="false"
          >
            <svg
              viewBox="0 0 24 24"
              xmlns="http://www.w3.org/2000/svg"
              class="icon icon--base icon--ArrowRight btn__icon"
            >
              <path
                xmlns="http://www.w3.org/2000/svg"
                d="m16.444 19.204 4.066-7.044-4.066-7.044-.65.375 3.633 6.294h-15.187v.75h15.187l-3.633 6.294z"
              />
            </svg>
            <span class="btn__text">{{ t('message.handbook.read_more') }}</span>
          </NuxtLinkLocale>
        </template>
      </OdsCard>
    </OdsSearchResults>
  </OdsPage>
</template>
