<script setup>
import {useRouter} from "vue-router";
import OdsBreadcrumbs from "../../app/components/OdsBreadcrumbs.vue";
import OdsPage from "../../app/components/OdsPage.vue";
import OdsSearchPanel from "../../app/components/OdsSearchPanel.vue";
import {homePageBreadcrumb} from "../../app/composables/breadcrumbs.js";

const { t, locale } = useI18n();
const route = useRoute();
const router = useRouter()

const searchInput = ref(route.query.q);

const onSearch = () => {
  if (searchInput.value.trim() !== '') {
    router.push({
      name: route.name,
      query: { q: searchInput.value.trim() },
    })
  }
};

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
</script>

<template>
  <OdsPage>
    <template #header>
      <OdsBreadcrumbs :breadcrumbs="breadcrumbs" />
      <OdsSearchPanel
        :search-input="searchInput"
        :search-prompt="t('message.handbook.search_prompt')"
        @search="onSearch"
        @update:search-input="value => searchInput = value"
      />
    </template>

    Foobar
  </OdsPage>
</template>
