<template>
      <span class="dataset-label">{{ t('message.dataset_detail.dataset') }}</span>

  <p class="meta-info">
    <span
      v-if="props.dataset.getCreated"
      class="meta-info__item"
      style="cursor: pointer"
      :title="showRaw ? t('message.dataset_detail.show_relative') : t('message.dataset_detail.show_raw')"
      :aria-label="showRaw ? t('message.dataset_detail.show_relative') : t('message.dataset_detail.show_raw')"
      @click="toggleRaw"
    >
      {{ t('message.dataset_detail.published_on') }}
      <NuxtTime
        :datetime="new Date(props.dataset.getCreated)"
        :locale="locale"
        :relative="!showRaw"
      />
    </span>
    <span
      v-if="props.dataset.getModified"
      class="meta-info__item"
      style="cursor: pointer"
      :title="showRaw ? t('message.dataset_detail.show_relative') : t('message.dataset_detail.show_raw')"
      :aria-label="showRaw ? t('message.dataset_detail.show_relative') : t('message.dataset_detail.show_raw')"
      @click="toggleRaw"
    >
      {{ t('message.dataset_detail.modified_on') }}
      <NuxtTime
        :datetime="new Date(props.dataset.getModified)"
        :locale="locale"
        :relative="!showRaw"
      />
    </span>
     <span
      v-if="props.dataset.getOdsAccrualPeriodicity"
      class="meta-info__item"
    >
      {{ t('message.dataset_detail.accrual_periodicity') }} <a class="link--external" :href="props.dataset.getOdsAccrualPeriodicity.resource">{{ props.dataset.getOdsAccrualPeriodicity.label }}</a>
    </span>
  </p>
  <p class="meta-info catalog-meta-info">
      <OdsDatasetCatalogPanel style="display: inline;" :dataset="props.dataset" />
  </p>

</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import type { Dataset } from '~/model/dataset'
import OdsDatasetCatalogPanel from './OdsDatasetCatalogPanel.vue'

const { locale, t } = useI18n()

const showRaw = ref(false)

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
