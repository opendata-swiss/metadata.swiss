<template>
  <div
    v-if="props.dataset"
    class="fileds"
  >
    <div
      class="filed"
    >
      <div class="filed-title">
        {{ t('Zuletzt aktualisiert') }}
      </div>
      <div>
        <OdsRelativeDateToggle
          v-if="props.dataset.modificationDate"
          :date="props.dataset.modificationDate"
        />
        <OdsRelativeDateToggle
          v-if="props.dataset.releaseDate"
          :date="props.dataset.releaseDate"
        />
        <div>update requency: {{ props.dataset.frequency?.label }}</div>
      </div>
    </div>
    <div
      class="filed"
    >
      <div class="filed-title">
        Nutzungsbedingungen
      </div>
      <div
        v-for="license in props.dataset.licenses"
        :key="license.id"
      >
        <OdsTermsOfUseIcon
          :license="license"
          class="license-icon"
        />
      </div>
    </div>
    <div
      class="filed"
    >
      <div class="filed-title">
        Organisation
      </div>
      <div>
        <span>Bundesamt für Orgs</span>
      </div>
    </div>
    <div
      class="filed"
    >
      <div class="filed-title">
        Kathegorien
      </div>
      <div>
        <div
          v-for="category in props.dataset.getCategoriesForLanguage(locale)"
          :key="category.id"
        >
          {{ category.label }}
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useI18n } from '#imports'

import OdsTermsOfUseIcon from './OdsTermsOfUseIcon.vue'
import OdsRelativeDateToggle from '../OdsRelativeDateToggle.vue'
import type { DcatApChV2DatasetAdapter } from './model/dcat-ap-ch-v2-dataset-adapter.js'

const { locale, t } = useI18n()

interface Props {
  dataset: DcatApChV2DatasetAdapter | undefined
}

const props = defineProps<Props>()
</script>

<style lang="scss" scoped>
.fileds {
  display: flex;
  flex-direction: row;
  gap: 12px;
  width: 100%;
  align-items: start;
  justify-content: space-around;
  border-bottom: 1px solid #e0e0e0;
  border-top: 1px solid #e0e0e0;
}

.filed-title {
  font-weight: bold;
}

.license-icon {
  height: 60px;
}
</style>
