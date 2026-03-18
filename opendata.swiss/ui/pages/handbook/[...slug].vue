<script lang="ts" setup>
import { loadHandbookBreadcrumb } from '~/lib/breadcrumbs'
import OdsHandbookPage from '../../app/components/handbook/OdsHandbookPage.vue'

const { locale, t } = useI18n()
const route = useRoute()

const breadcrumbs = await useBreadcrumbs({
  route,
  locale,
  loadContent: loadHandbookBreadcrumb(locale),
})

const { data } = useAsyncData(route.path, async () => {
  const path = [...route.params.slug]
  let slug = path.pop()

  const page = await queryCollection('handbook')
    .where('path', 'LIKE', `%.${locale.value}`)
    .where('slug', '=', slug).first()

  while (path.length) {
    const parentPage = await queryCollection('handbook')
      .where('slug', '=', slug)
      .first()
    if (!parentPage) {
      return undefined
    }
    slug = path.pop()
  }

  return page
})

useSeoMeta({
  title: `${data.value?.title} | ${t('message.header.navigation.handbook')} | opendata.swiss`,
})

// TODO: make sidebar navigation dynamic based on the handbook content
const _navigation = ref([
  {
    title: 'Guide',
    icon: 'i-lucide-book-open',
    path: '#getting-started',
    children: [
      {
        title: 'Introduction',
        path: '#introduction',
        active: true,
      },
      {
        title: 'Installation',
        path: '#installation',
      },
    ],
  },
  {
    title: 'Composables',
    icon: 'i-lucide-database',
    path: '#composables',
    children: [
      {
        title: 'defineShortcuts',
        path: '#defineshortcuts',
      },
      {
        title: 'useModal',
        path: '#usemodal',
      },
    ],
  },
])
</script>

<template>
  <OdsHandbookPage
    :page="data"
    :breadcrumbs="breadcrumbs"
  />
</template>
