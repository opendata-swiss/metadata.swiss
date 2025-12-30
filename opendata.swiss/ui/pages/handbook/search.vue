<script setup>
import {useRouter} from 'vue-router';
import Fuse from 'fuse.js'
import OdsBreadcrumbs from '../../app/components/OdsBreadcrumbs.vue';
import OdsPage from '../../app/components/OdsPage.vue';
import OdsSearchPanel from '../../app/components/OdsSearchPanel.vue';
import {homePageBreadcrumb} from '../../app/composables/breadcrumbs.js';
import OdsCard from '../../app/components/OdsCard.vue';
import OdsSearchResults from '../../app/components/OdsSearchResults.vue';
import {useSeoMeta} from 'nuxt/app';

const {t, locale} = useI18n();
const route = useRoute();
const router = useRouter()

const searchInput = ref(route.query.q);

const onSearch = (value) => {
  searchInput.value = value;
  router.push({
    name: route.name,
    query: {q: value.trim()},
  })
};

const {data} = await useAsyncData('handbook-search', async () => {
  const sections = await queryCollectionSearchSections('handbook');
  const pages = await queryCollection('handbook')
    .select('path', 'permalink', 'section')
    .all()

  console.log(pages)

  return sections.map(section => {
    let path
    const [id, hash] = section.id.split('#')

    const page = pages.find(({ path }) => section.id.startsWith(path))
    if (page) {
      path = `${page.section.toLowerCase()}/${page.permalink}`
    } else {
       path = id.substring(0, id.lastIndexOf('.'))
    }

    return {
      ...section,
      path,
      hash: hash ? `#${hash}` : null,
    };
  });
})

const fuse = new Fuse(data.value, {
  keys: ['title', 'titles', 'content'],
  ignoreDiacritics: true,
})

const articleIdPattern = new RegExp(`\\.${locale.value}(#.+)?`)
const result = computed(() => {
  return fuse
    .search(toValue(searchInput))
    .filter(article =>
      article.item.content
      && article.item.level < 3
      && articleIdPattern.test(article.item.id)
    )
    .slice(0, 10);
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
    searchInput.value = value;
  }
);

useSeoMeta({
  title: `${t('message.header.navigation.search')} | ${t('message.header.navigation.handbook')} | opendata.swiss`,
})
</script>

<template>
  <OdsPage>
    <template #header>
      <OdsBreadcrumbs :breadcrumbs="breadcrumbs"/>
      <OdsSearchPanel
        :search-input="searchInput"
        :search-prompt="t('message.handbook.search_prompt')"
        @search="onSearch"
      />
    </template>
    <OdsSearchResults :results-count="result.length">
      <OdsCard
        v-for="article in result"
        :key="article.item.id"
        :title="article.item.title"
        type="list"
        clickable
      >
        <p>{{ article.item.content }}</p>

        <template #footer-action>
          <NuxtLinkLocale
            :to="article.item"
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
