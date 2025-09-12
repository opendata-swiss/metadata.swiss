<script setup>
import OdsPage from "../../app/components/OdsPage.vue";
import {homePageBreadcrumb} from "../../app/composables/breadcrumbs.js";
import SvgIcon from "../../app/components/SvgIcon.vue";
import OdsBreadcrumbs from "../../app/components/OdsBreadcrumbs.vue";
import OdsCard from "../../app/components/OdsCard.vue";
import OdsPagination from "../../app/components/OdsPagination.vue";

const route = useRoute()
const {locale, t} = useI18n()
const localePath = useLocalePath()

const PAGE_SIZE = 1 // Number of posts per page
const page = parseInt(route.params.page) || 1

const {data} = await useAsyncData(route.path, () => {
  return queryCollection('blog')
    .where('path', 'LIKE', `%.${locale.value}`)
    .order('date', 'DESC')
    .skip((page - 1) * PAGE_SIZE)
    .limit(PAGE_SIZE)
    .all()
})
const {data: pageCount} = await useAsyncData(route.path + '_pageCount', () => {
  return queryCollection('blog')
    .where('path', 'LIKE', `%.${locale.value}`)
    .order('date', 'DESC')
    .count()
})

useSeoMeta({
  title: `${t('message.header.navigation.blog')} | opendata.swiss`,
})

const posts = computed(() => {
  return data.value?.map(post => {
    const date = new Date(post.date)

    return {
      ...post,
      year: date.getFullYear(),
      month: date.getMonth() + 1,
      slug: post.slug || post.path.split('/').pop().replace(`.${locale.value}`, ''),
    };
  }) || []
})

const breadcrumbs = [
  await homePageBreadcrumb(locale),
  {
    title: t('message.header.navigation.blog'),
    path: '/blog',
  },
]
</script>

<template>
  <OdsPage>
    <template #header>
      <OdsBreadcrumbs :breadcrumbs="breadcrumbs"/>
    </template>
    <section class="section section--default">
      <div class="container gap--responsive">
        <div class="search-results search-results--grid" aria-live="polite" aria-busy="false">
          <ul class="search-results-list">
            <li v-for="post in posts" :key="post.id">
              <OdsCard :title="post.title" type="universal" clickable>
                <template #top-meta>
                    <NuxtTime class="meta-info__item" :datetime="new Date(post.date)" relative />
                </template>
                <template #footer-action>
                  <NuxtLinkLocale
                    :to="{ name: 'blog-year-month-slug', params: { year: post.year, month: post.month, slug: post.slug } }"
                    type="false" class="btn btn--outline btn--icon-only" aria-label="false">
                    <SvgIcon icon="ArrowRight" role="btn" />
                    <span class="btn__text">Weiterlesen</span>
                  </NuxtLinkLocale>
                </template>
              </OdsCard>
            </li>
          </ul>
        </div>
      </div>
      <div class="container">
        <OdsPagination
          :current-page="page"
          :page-label="t('message.ods-pagination.page')"
          :total-pages="parseInt(pageCount)"
          :total-pages-label="t('message.ods-pagination.of', { pageCount })"
          :pagination-items="[
            {
              icon: 'ChevronLeft',
              label: t('message.ods-pagination.previous'),
              link: `/blog/${page > 1 ? parseInt(page) - 1 : 1}`,
            },
            {
              icon: 'ChevronRight',
              label: t('message.ods-pagination.next'),
              link: `/blog/${page < pageCount ? parseInt(page) + 1 : pageCount}`
            }
          ]"
          @page-change="navigateTo(localePath({ name: 'blog-page', params: { page: $event } }))"
        />
      </div>
    </section>
  </OdsPage>
</template>
