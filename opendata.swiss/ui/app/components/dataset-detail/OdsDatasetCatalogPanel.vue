<template>
  <div class="wrapper">
  <div class="button-container">
      {{ t('message.dataset_detail.catalog') }}

  <OdsButton
    variant="link"
    :title="showCatalogInfo ? t('message.dataset_detail.hide_catalog_entry') : t('message.dataset_detail.show_catalog_entry')"
    :aria-label="showCatalogInfo ? t('message.dataset_detail.hide_catalog_entry') : t('message.dataset_detail.show_catalog_entry')"
    size="sm"
    icon-only
    @click="showCatalogInfo = !showCatalogInfo"
  >
          {{ showCatalogInfo ? props.dataset.getOdsCatalogInfo.title : props.dataset.getOdsCatalogInfo.title }}

    <SvgIcon
      icon="ChevronDown"
      role="btn"
      :class="{ 'rotated': showCatalogInfo }"
    />

  </OdsButton>
</div>

<div v-if="showCatalogInfo">
  <p  class="meta-info">
      <span class="meta-info__item">{{ t('message.dataset_detail.catalog') }}</span>
    <span
      v-if="props.dataset.getOdsCatalogInfo.issued"
      class="meta-info__item"
      :title="showRaw ? t('message.dataset_detail.show_relative') : t('message.dataset_detail.show_raw')"
      :aria-label="showRaw ? t('message.dataset_detail.show_relative') : t('message.dataset_detail.show_raw')"
      @click="toggleRaw"
    >
      {{ t('message.dataset_detail.created_on') }}
      <NuxtTime
        :datetime="new Date(props.dataset.getOdsCatalogInfo.issued)"
        :relative="!showRaw"
        :locale="locale"
      />
    </span>
    <span
      v-if="props.dataset.getOdsCatalogInfo.modified"
      class="meta-info__item"
      :title="showRaw ? t('message.dataset_detail.show_relative') : t('message.dataset_detail.show_raw')"
      :aria-label="showRaw ? t('message.dataset_detail.show_relative') : t('message.dataset_detail.show_raw')"
      @click="toggleRaw"
    >
      {{ t('message.dataset_detail.modified_on') }}
      <NuxtTime
        :datetime="new Date(props.dataset.getOdsCatalogInfo.modified)"
        :relative="!showRaw"
        :locale="locale"
      />
    </span>
  </p>
  <p class="meta-info">
          <span class="meta-info__item">{{ t('message.dataset_detail.this_dataset_record') }}</span>

    <span
      v-if="props.dataset.getOdsCatalogInfo.record?.issued"
      class="meta-info__item"
      :title="showRaw ? t('message.dataset_detail.show_relative') : t('message.dataset_detail.show_raw')"
      :aria-label="showRaw ? t('message.dataset_detail.show_relative') : t('message.dataset_detail.show_raw')"
      @click="toggleRaw"
    >
      {{ t('message.dataset_detail.created_on') }}
      <NuxtTime
        :datetime="new Date(props.dataset.getOdsCatalogInfo.record.issued)"
        :relative="!showRaw"
        :locale="locale"
      />
    </span>
    <span
      v-if="props.dataset.getOdsCatalogInfo.record?.modified"
      class="meta-info__item"
      style="cursor: pointer"
      :title="showRaw ? t('message.dataset_detail.show_relative') : t('message.dataset_detail.show_raw')"
      :aria-label="showRaw ? t('message.dataset_detail.show_relative') : t('message.dataset_detail.show_raw')"
      @click="toggleRaw"
    >
      {{ t('message.dataset_detail.modified_on') }}
      <NuxtTime
        :datetime="new Date(props.dataset.getOdsCatalogInfo.record.modified)"
        :relative="!showRaw"
        :locale="locale"
      />
    </span>
  </p>
  </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import type { Dataset } from '~/model/dataset'
import OdsButton from '../OdsButton.vue'

const { locale, t } = useI18n()

const showRaw = ref(false)
const showCatalogInfo = ref(false)
const props = defineProps({
  dataset: {
    type: Object as () => Dataset,
    required: true
  }
})

function toggleRaw() {
  showRaw.value = !showRaw.value
}
</script>

<style lang="scss" scoped>
.wrapper {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  align-items: flex-start;
  justify-content: flex-start;
}
.button-container {
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: flex-start;


}
.rotated {
  transform: rotate(180deg);
  transition: transform 0.2s;
}

.meta-info__item {
  cursor: pointer;
}

</style>
