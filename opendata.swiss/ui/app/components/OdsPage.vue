<script setup lang="ts">
import { Comments } from '@hyvor/hyvor-talk-vue'
import OdsToc from '~/components/OdsToc.vue'
import Hero from '~/components/OdsHero.vue'
import type { PagesCollectionItem } from '@nuxt/content'

const { locale } = useI18n()

const { comments: { websiteId } } = useRuntimeConfig().public
type Page = Pick<PagesCollectionItem, 'heading' | 'title' | 'subHeading' | 'heroImage' | 'noToc'> & Partial<Pick<PagesCollectionItem, 'body'>>

const { page } = defineProps<{
  page?: Page
  commentsId?: string
}>()
</script>

<template>
  <header id="main-header">
    <slot name="header" />
  </header>
  <div id="main-content">
    <Hero
      v-if="page"
      :type="page.heroImage ? 'main-image' : 'default'"
    >
      <template #title>
        {{ page.heading || page.title }}
      </template>
      <template #description>
        <slot name="hero-subheading">
          <MDC
            v-if="page.subHeading"
            :value="page.subHeading"
          />
        </slot>
      </template>

      <template
        v-if="page.heroImage"
        #image
      >
        <NuxtImg :src="page.heroImage" />
      </template>
    </Hero>
    <slot>
      <section
        v-if="page"
        class="section section--py"
      >
        <ContentRenderer
          v-if="page.noToc"
          :value="page"
        />
        <div
          v-else
          class="container container--grid container--reverse-mobile gap--responsive"
        >
          <div class="container__main vertical-spacing">
            <ContentRenderer :value="page" />
          </div>
          <div class="container__aside">
            <div
              id="aside-content"
              class="sticky sticky--top"
            >
              <slot name="before-aside-content" />
              <OdsToc
                v-if="page?.body?.toc?.links.length"
                :toc="page.body.toc"
              />
              <slot name="aside-content" />
            </div>
          </div>
        </div>

        <div
          v-if="commentsId"
          class="container"
        >
          <Comments
            :website-id="websiteId"
            :page-id="commentsId"
            :page-language="locale"
          />
        </div>
      </section>
    </slot>
  </div>
</template>
