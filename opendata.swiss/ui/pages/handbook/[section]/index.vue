<script setup>
import toProperCase from '~/lib/toProperCase.js';
import { loadHandbookSectionBreadcrumb } from '../../../app/lib/breadcrumbs.js';
import OdsHandbookPage from '../../../app/components/handbook/OdsHandbookPage.vue';

const { locale } = useI18n()
const route = useRoute()
const section = toProperCase(route.params.section)

const { data } = await useAsyncData('handbookSection', () =>
  queryCollection('handbookSections')
    .where('title', '=', section)
    .first())

const breadcrumbs = await useBreadcrumbs({
  route,
  locale,
  loadContent: loadHandbookSectionBreadcrumb(section, locale),
})
</script>

<template>
  <OdsHandbookPage :page="data" :breadcrumbs="breadcrumbs" />
</template>
