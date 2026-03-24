<template>
  <section
    class="section section--default bg--secondary-50"
    :aside="aside ? 'true' : 'false'"
  >
    <div class="container">
      <h3
        v-if="aside"
        class="h3"
      >
        {{ title || t('message.dataset_search.search_results') }}
      </h3>
      <h1
        v-else
        class="h1"
      >
        {{ title || t('message.dataset_search.search_results') }}
      </h1>
      <div class="search search--large search--page-result">
        <div class="search__group">
          <input
            id="search-input"
            ref="_inputElement"
            v-model="modelValue"
            :placeholder="searchPrompt"
            type="search"
            autocomplete="off"
            class="search"
            @keyup.enter="onSearch"
          >
          <OdsButton
            variant="bare"
            :title="t('message.dataset_search.search_button')"
            size="lg"
            icon="Search"
            icon-only
            @click="onSearch"
          />
        </div>
      </div>
      <div
        v-if="facetRefs && activeFacets"
        class="search__filters"
      >
        <OdsFilterPanel
          :facet-refs="facetRefs"
          :facets="activeFacets"
          @reset-all-facets="$emit('reset-all-facets')"
        />
      </div>
      <div
        v-if="facetRefs && activeFacets"
        class="filters__active"
      />
    </div>
  </section>
</template>

<script setup lang="ts">
import OdsButton from '~/components/OdsButton.vue'
import OdsFilterPanel from '~/components/dataset/OdsFilterPanel.vue'
import type { SearchResultFacetGroupLocalized } from '@piveau/sdk-vue'

const { t } = useI18n()

interface PropTypes {
  searchInput: string | string[]
  searchPrompt: string
  aside?: boolean
  title?: string
  facetRefs?: Record<string, Ref<string[]>>
  activeFacets?: SearchResultFacetGroupLocalized[]
}

const props = defineProps<PropTypes>()

const emit = defineEmits({
  'search': (_: string) => true,
  'reset-all-facets': () => true,
  'update:searchInput': (_: string | string[]) => true,
})

const modelValue = computed({
  get: () => {
    if (Array.isArray(props.searchInput)) {
      return props.searchInput[0] || ''
    }
    return props.searchInput || ''
  },
  set: (value: string) => emit('update:searchInput', value),
})

const _inputElement = ref<HTMLInputElement | null>(null)

const onSearch = () => {
  emit('search', modelValue.value?.trim() || '')
}
</script>

<style scoped>
section[aside] .search {
  width: 100%
}

h3 {
  margin-top: 0;
}
</style>
