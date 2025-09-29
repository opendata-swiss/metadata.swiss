<template>
  <div class="search__filters__tags">
    <OdsTagItem
      v-for="filter in activeFilter"
      :id="filter.key"
      :key="filter.key"
      :label="filter.value"
      variant="primary"
      size="sm"
      icon="Cancel"
      @click="resetFacet(filter.key)"
    />
    <OdsTagItem
      v-if="activeFilter.length > 0"
      id="reset"
      label="Filter zurücksetzen"
      variant="default"
      size="sm"
      icon="Repeat"
      @click.prevent="resetAllFilters"
    />
  </div>
</template>

<script setup lang="ts">
import type { PropType, Ref } from 'vue'
import OdsTagItem from '../OdsTagItem.vue'
import type { SearchResultFacetGroupLocalized } from '@piveau/sdk-vue'

interface FilterEntry {
  key: string
  value: string
}
const props = defineProps({
  facetRefs: {
    type: Object as PropType<Record<string, Ref<string[]>>>,
    required: true,
  },
   facets: {
    type: Array as PropType<SearchResultFacetGroupLocalized[]>,
    required: true,
  },
})

const emit = defineEmits<{
  (e: 'reset-all-facets'): void
}>()

const activeFilter = computed<FilterEntry[]>(() => {
  const facetRefs = props.facetRefs
  const filters = Object.keys(facetRefs).flatMap((key) => {
    const values = facetRefs[key]!.value
    if (values.length === 0) {
      return []
    }
    const valueTitles = values.map(val => {
      const facetItems = props.facets.find(f => f.id === key)?.items
      return facetItems?.find(item => item.id === val)?.title || val
    })
    return {key, value: valueTitles.join(', ')}
  })
  filters.sort((a, b) => a.key.localeCompare(b.key))
  return filters
})

function resetFacet(facetId: string) {
  const facet = props.facetRefs[facetId]
  if (facet) {
    facet.value = []
  }
}

function resetAllFilters() {
  emit('reset-all-facets')
}
</script>
