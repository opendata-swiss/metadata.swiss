<template>
  <span class="dataset-label">{{ t('message.dataset_detail.dataset') }}</span>
  <p class="meta-info">
    <span
      v-if="props.dataset.releaseDate"
      class="meta-info__item"
    >
      {{ t('message.dataset_detail.released') }}
      <OdsRelativeDateToggle :date="props.dataset.releaseDate" />
    </span>
    <span
      v-if="props.dataset.modificationDate"
      class="meta-info__item"
    >
      {{ t('message.dataset_detail.modified_on') }}
      <OdsRelativeDateToggle :date="props.dataset.modificationDate" />
    </span>
     <span
      v-if="props.dataset.frequency"
      class="meta-info__item"
    >
      {{ t('message.dataset_detail.frequency') }}
      <a class="link--external" target="_blank" :href="props.dataset.frequency.resource">{{ props.dataset.frequency.label }}</a>
    </span>
  </p>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import type { DcatApChV2DatasetAdapter } from './model/dcat-ap-ch-v2-dataset-adapter'
import OdsRelativeDateToggle from '../OdsRelativeDateToggle.vue'

const { t } = useI18n()


const props = defineProps({
  dataset: {
    type: Object as PropType<DcatApChV2DatasetAdapter>,
    required: true
  }
})

</script>

<style lang="scss" scoped>

.catalog-meta-info {
  margin-top: 0 !important;
  display: flex;
  flex-direction: row;
  align-items: center;
}

.dataset-label {
  position: relative;
  background-color: #e6f0fa;
  color: #1976d2;
  padding: 2px 10px;
  border-radius: 6px;
  font-weight: 600;
  letter-spacing: 0.03em;
  display: inline-block;
  margin-right: 10px;
  vertical-align: middle;
  border: 1px solid #b3d4fc;
}

</style>
