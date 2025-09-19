<script lang="ts" setup>
import OdsBreadcrumbs from "~/components/OdsBreadcrumbs.vue";
import toProperCase from "~/lib/toProperCase.js";
import { loadHandbookBreadcrumb } from "~/lib/breadcrumbs";
import OdsPage from "../../../app/components/OdsPage.vue";

const { locale, t } = useI18n();
const route = useRoute()

const section = toProperCase(route.params.section)

const breadcrumbs = await useBreadcrumbs({
  route,
  locale,
  loadContent: loadHandbookBreadcrumb(section, locale)
})

const { data } = await useAsyncData(route.path, () => {
  const slug = route.params.slug.join('/')

  return queryCollection('handbook')
    .where('path', 'LIKE',`%.${locale.value}`)
    .where('section', '=', section)
    .andWhere(q => q.where('permalink', '=', slug))
    .first()
})

useSeoMeta({
  title: `${data.value?.title} | ${t('message.header.navigation.handbook')} | opendata.swiss`,
})

// TODO: make sidebar navigation dynamic based on the handbook content
const navigation = ref([
  {
    title: 'Guide',
    icon: 'i-lucide-book-open',
    path: '#getting-started',
    children: [
      {
        title: 'Introduction',
        path: '#introduction',
        active: true
      },
      {
        title: 'Installation',
        path: '#installation'
      }
    ]
  },
  {
    title: 'Composables',
    icon: 'i-lucide-database',
    path: '#composables',
    children: [
      {
        title: 'defineShortcuts',
        path: '#defineshortcuts'
      },
      {
        title: 'useModal',
        path: '#usemodal'
      }
    ]
  }
])
</script>

<template>
  <OdsPage v-if="data" :page="data" >
    <template #header>
      <OdsBreadcrumbs :breadcrumbs="breadcrumbs" />
    </template>
  </OdsPage>
</template>
