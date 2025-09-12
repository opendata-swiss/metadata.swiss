<script setup>
import OdsPage from "../../app/components/OdsPage.vue";
import {homePageBreadcrumb} from "../../app/composables/breadcrumbs.js";
import OdsBreadcrumbs from "../../app/components/OdsBreadcrumbs.vue";
import OdsCard from "../../app/components/OdsCard.vue";
import SvgIcon from "../../app/components/SvgIcon.vue";

const { locale, t } = useI18n()
const breadcrumbs = [
  await homePageBreadcrumb(locale),
  {
    title: t('message.header.navigation.showcases'),
    path: '/showcases',
  },
]
useSeoMeta({
  title: `${t('message.header.navigation.showcases')} | opendata.swiss`,
})

const { data: showcases } = await useAsyncData('showcases_' + locale.value, () => {
  return queryCollection('showcases')
    .where('path', 'LIKE', `%.${locale.value}`)
    .where('active', '==', true)
    .all()
})

function firstParagraph(showcase) {
  const [firstPara] = showcase.body.value
  let body = [firstPara]
  if (!firstPara) {
    body = []
  }

  return {
    ...showcase,
    body: {
      ...showcase.body,
      value: body
    },
  }
}

function showcaseId(showcase) {
  return showcase.id.replace(/.*?([^/]+)\.\w\w\.md$/, '$1')
}
</script>

<template>
  <OdsPage>
    <template #header>
      <OdsBreadcrumbs :breadcrumbs="breadcrumbs" />
    </template>
    <section class="section section--py">
      <div class="container container--grid container--reverse-mobile gap--responsive">
        <div class="container__main vertical-spacing">
          <OdsCard
            v-for="showcase in showcases"
            :key="showcase.id"
            :title="showcase.title"
            clickable
          >
            <template #image>
              <img :src="showcase.image" :alt="showcase.title" >
            </template>

            <template #top-meta>
              <div>
                <span class="meta-info__item">{{ showcase.type }}</span>
              </div>
            </template>

            <ContentRenderer :value="firstParagraph(showcase)" />

            <template #footer-action>
              <NuxtLinkLocale :to="{ name: 'showcase-id', params: { id: showcaseId(showcase) } }" type="false" class="btn btn--outline btn--icon-only" aria-label="false">
                <SvgIcon icon="ArrowRight" role="btn" />
                <span class="btn__text">Weiterlesen</span>
              </NuxtLinkLocale>
            </template>
          </OdsCard>
        </div>
      </div>
    </section>
  </OdsPage>
</template>
