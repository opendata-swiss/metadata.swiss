<script setup lang="ts">
import OdsSection, { type SectionLayout } from './OdsSection.vue'
import OdsShowcaseCard from '~/components/showcases/OdsShowcaseCard.vue'
import { useShowcaseSearch } from '~/piveau/showcases'

const { showcases, max } = defineProps<{
  title: string
  max: number
  showcases: Array<{ id: string, label: string }>
  layout: SectionLayout
}>()

const { useResource, useSearch } = useShowcaseSearch()

const loading = showcases.map(({ id }) => {
  return useResource(id)
})

const loaded = (await Promise.all(loading.map(async ({ query, resultEnhanced }) => {
  await query.suspense()

  return resultEnhanced
}))).map(({ value }) => value).filter(Boolean)

const { query, getSearchResultsEnhanced } = useSearch({
  queryParams: {
    limit: max - loaded.length,
  },
  additionalParams: {
    resource: 'showcase',
  },
})

await query.suspense()

const combined = computed(() => {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  return [...loaded, ...getSearchResultsEnhanced.value as any] as any
})
</script>

<template>
  <OdsSection
    :layout="layout"
    accent-color="100"
    text-color="600"
    :title="title"
  >
    <OdsShowcaseCard
      v-for="showcase in combined"
      :key="showcase.id"
      :showcase="showcase"
      no-excerpt
      :slideshow-card="layout === 'slideshow'"
    />
  </OdsSection>
</template>
