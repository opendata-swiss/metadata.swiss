<template>
  <section class="section section--default bg--secondary-50">
    <div class="container">
      <h1 class="h1">{{ t('message.dataset_search.search_results') }}</h1>
      <div class="search search--large search--page-result">
        <div class="search__group">
          <input
            id="search-input"
            :value="searchInput"
            :placeholder="t('message.dataset_search.search_placeholder')"
            type="search"
            :label="t('message.dataset_search.search_placeholder')"
            autocomplete="off"
            class="search"
            @input="$emit('update:searchInput', $event.target.value)"
            @keyup.enter="$emit('search')"
          >
          <OdsButton
            variant="bare"
            :title="t('message.dataset_search.search_button')"
            size="lg"
            icon="Search"
            icon-only
            @click="$emit('search')"
          />
        </div>
      </div>
      <div class="search__filters">
        <OdsFilterPanel :facet-refs="facetRefs" :facets="activeFacets" @reset-all-facets="$emit('reset-all-facets')" />
      </div>
      <div class="filters__active" />
    </div>
  </section>
</template>

<script setup lang="ts">
import OdsButton from "~/components/OdsButton.vue";
import OdsFilterPanel from "~/components/dataset/OdsFilterPanel.vue";
import type {SearchResultFacetGroupLocalized} from "@piveau/sdk-vue";
const { t } = useI18n()

interface PropTypes {
  searchInput: Ref<string | string[]>;
  facetRefs: Record<string, Ref<string[]>>;
  activeFacets: SearchResultFacetGroupLocalized[];
}

defineProps<PropTypes>()

defineEmits({
  search: () => true,
  'reset-all-facets': () => true,
  'update:searchInput': (_: string | string[]) => true,
})
</script>
