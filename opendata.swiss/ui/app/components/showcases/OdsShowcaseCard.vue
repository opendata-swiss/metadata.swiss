<template>
  <OdsCard
    style="height: 100%;"
    :title="getCurrentTranslation(showcase.title, locale.value)"
    clickable
  >
    <template #image>
      <img
        :src="showcase.image[0]"
        :alt="getCurrentTranslation(showcase.title, locale.value)"
      >
    </template>

    <template #top-meta>
      <div>
        <span
          v-if="showcaseType(showcase)"
          class="meta-info__item"
        >{{ showcaseType(showcase)!.pref_label }}</span>
        <span class="meta-info__item">
          {{ t('message.showcase.search.dataset_references', { count: showcase.references.length }) }}
        </span>
      </div>
    </template>

    <template #footer-info>
      <div>
        <span
          v-for="tag in showcase.keywords"
          :key="tag.id"
          class="tag"
        >
          {{ tag.label }}
        </span>
      </div>
    </template>

    <MDC :value="getCurrentTranslation(showcase.abstract, locale.value)" />

    <template #footer-action>
      <NuxtLinkLocale
        :to="{ name: 'showcase-id', params: { id: showcase.id } }"
        type="false"
        class="btn btn--outline btn--icon-only"
        aria-label="false"
      >
        <SvgIcon
          icon="ArrowRight"
          role="btn"
        />
        <span class="btn__text">Weiterlesen</span>
      </NuxtLinkLocale>
    </template>
  </OdsCard>
</template>

<script setup lang="ts">
import OdsCard from '~/components/content/OdsCard.vue'
import SvgIcon from '~/components/SvgIcon.vue'
import { getCurrentTranslation } from '~/lib/getCurrentTranslation'
import type { ShowcasesCollectionItem } from '@nuxt/content'
import { useShowcaseTypes } from '~~/composables/useShowcaseTypes'

const { locale, t } = useI18n()

const { showcase } = defineProps<{
  showcase: ShowcaseCollectionItem
}>()

console.log(showcase)

const { data: showcaseTypes, ensureLoaded } = useShowcaseTypes()
await ensureLoaded()

function showcaseType(showcase: ShowcasesCollectionItem) {
  return showcaseTypes.value?.find(type => type.resource === showcase.type)
}
</script>
