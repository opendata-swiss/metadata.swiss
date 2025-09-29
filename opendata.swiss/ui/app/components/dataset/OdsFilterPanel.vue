<template>

<OdsButton
    variant="link"
    :title="showFilters ? t('message.dataset_search.hide_filters') : t('message.dataset_search.show_filters')"
    :aria-label="showFilters ? t('message.dataset_search.hide_filters') : t('message.dataset_search.show_filters')"
    size="sm"
    @click="showFilters = !showFilters"
  >
    <template #icon>
      <SvgIcon
        icon="ChevronDown"
        role="btn"
        :class="{ 'rotated': showFilters }"
      />
    </template>
    {{ showFilters ? t('message.dataset_search.hide_filters') : t('message.dataset_search.show_filters') }}
  </OdsButton>

  <ClientOnly>
    <div v-show="showFilters" class="search__filters__drawer">
      <OdsMultiSelect
        v-for="facet in props.facets"
        :key="facet.id"
        :facet="facet"
        :options="facet.items"
        :label="facet.title"
        :selected="facetRefs[facet.id]?.value"
        @update:model-value="handleFacetChange(facet, $event)"
      />
    </div>
    <div v-show="!showFilters" class="filters__active">
      <OdsActiveFilters :facets="props.facets" :facet-refs="facetRefs" @reset-all-facets="emit('reset-all-facets')" />
    </div>
  </ClientOnly>

</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'

import type { SearchResultFacetGroupLocalized } from '@piveau/sdk-vue';
import OdsMultiSelect from './OdsMultiSelect.vue';
import OdsButton from '../OdsButton.vue';
import OdsActiveFilters from './OdsActiveFilters.vue';
import SvgIcon from "~/components/SvgIcon.vue";

const props = defineProps({
  facets: {
    type: Array as PropType<SearchResultFacetGroupLocalized[]>,
    required: true,
  },
  facetRefs: {
    type: Object as PropType<Record<string, Ref<string[]>>>,
    required: true,
  }
})

const emit = defineEmits<{
  (e: 'reset-all-facets'): void
}>();

const { t } = useI18n()
const showFilters = ref(false)

function handleFacetChange(facet: SearchResultFacetGroupLocalized, value: string[]) {
  currentFilters.value.set(facet.id, value)
  if (props.facetRefs[facet.id]) {
    props.facetRefs[facet.id]!.value = value
  } else {
    console.warn(`Facet reference for ${facet.id} is not defined. Cannot update model with new value ${value}.`)
  }
}

const currentFilters = ref(new Map<string, string[]>(new Map()))

</script>

<style lang="scss" scoped>

.rotated {
  transform: rotate(180deg);
  transition: transform 0.2s;
}
</style>
