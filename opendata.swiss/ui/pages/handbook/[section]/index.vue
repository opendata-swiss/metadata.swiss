<script setup>
import toProperCase from "~/lib/toProperCase.js";
import { loadHandbookSectionBreadcrumb } from "../../../app/lib/breadcrumbs.js";
import OdsBreadcrumbs from "../../../app/components/OdsBreadcrumbs.vue";
import OdsPage from "../../../app/components/OdsPage.vue";

const { locale } = useI18n();
const route = useRoute();
const section = toProperCase(route.params.section);

const { data } = await useAsyncData('handbookSection', () =>
  queryCollection('handbookSections')
    .where('title', '=', section)
    .first());

const breadcrumbs = await useBreadcrumbs({
  route,
  locale,
  loadContent: loadHandbookSectionBreadcrumb(section, locale)
})
</script>

<template>
  <OdsPage v-if="data" :page="data" >
    <template #header>
      <OdsBreadcrumbs :breadcrumbs="breadcrumbs" />
    </template>
  </OdsPage>
</template>
