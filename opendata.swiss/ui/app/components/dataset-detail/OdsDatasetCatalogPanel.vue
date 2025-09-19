<template>
   <p class="meta-info">
      <span class="meta-info__item">{{ t('message.dataset_detail.catalog') }}</span>
      <span class="meta-info__item">{{ props.dataset.catalog.title }}</span>
   </p>
    <p class="meta-info">
      <span
        v-if="props.dataset.catalog.issued"
        class="meta-info__item"
      >
        {{ t('message.dataset_detail.created_on') }}
        <OdsRelativeDateToggle :date="new Date(props.dataset.catalog.issued)" />
      </span>
      <span
        v-if="props.dataset.catalog.modified"
        class="meta-info__item"
      >
        {{ t('message.dataset_detail.modified_on') }}
        <OdsRelativeDateToggle :date="new Date(props.dataset.catalog.modified)" />
      </span>
    </p>
    <p class="meta-info" style="margin-top: 1rem">
      <span class="meta-info__item">{{ t('message.dataset_detail.this_dataset_record') }}</span>
    </p>

    <p class="meta-info">
      <span
        v-if="props.dataset.catalog.record?.issued"
        class="meta-info__item"
      >
        {{ t('message.dataset_detail.created_on') }}
         <OdsRelativeDateToggle :date="new Date(props.dataset.catalog.record.issued)" />
      </span>
      <span
        v-if="props.dataset.catalog.record?.modified"
        class="meta-info__item"
        style="cursor: pointer"
      >
        {{ t('message.dataset_detail.modified_on') }}
        <OdsRelativeDateToggle :date="new Date(props.dataset.catalog.record.modified)" />
      </span>
    </p>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import type { DcatApChV2DatasetAdapter } from './model/dcat-ap-ch-v2-dataset-adapter'
import OdsRelativeDateToggle from '../OdsRelativeDateToggle.vue';

const { t } = useI18n()

const props = defineProps({
  dataset: {
    type: Object as PropType<DcatApChV2DatasetAdapter>,
    required: true
  }
})

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
  gap: 6px;
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
