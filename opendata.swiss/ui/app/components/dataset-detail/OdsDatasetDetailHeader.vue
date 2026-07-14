<template>
  <div
    v-if="props.dataset"
    class="metadata-header"
  >
    <!-- Column 1: Aktualisierung -->
    <div class="metadata-col">
      <div class="metadata-col-header">
        <SvgIcon
          icon="Clock"
          class="col-icon"
        />
        <span class="col-title">{{ t('message.dataset_detail.details') }}</span>
      </div>
      <div class="col-content">
        <div
          v-if="props.dataset.modificationDate"
          class="meta-item"
        >
          <span class="meta-label">{{ t('message.dataset_detail.modified_on') }}</span>
          <OdsRelativeDateToggle :date="props.dataset.modificationDate" />
        </div>
        <div
          v-if="props.dataset.releaseDate"
          class="meta-item"
        >
          <span class="meta-label">{{ t('message.dataset_detail.released') }}</span>
          <OdsRelativeDateToggle :date="props.dataset.releaseDate" />
        </div>
        <div
          v-if="props.dataset.frequency?.label"
          class="meta-item"
        >
          <span class="meta-label">{{ t('message.dataset_detail.frequency') }}</span>
          <span class="frequency-text">{{ props.dataset.frequency.label }}</span>
        </div>
      </div>
    </div>

    <!-- Column 2: Nutzungsbedingungen -->
    <div class="metadata-col">
      <div class="metadata-col-header">
        <SvgIcon
          icon="Certificate"
          class="col-icon"
        />
        <span class="col-title">{{ t('message.header.navigation.terms_of_use') }}</span>
      </div>
      <div class="col-content">
        <div class="license-wrapper">
          <div
            v-for="license in props.dataset.licenses"
            :key="license.id"
            class="license-item"
          >
            <OdsTermsOfUseIcon
              :license="license"
              class="license-icon"
            />
          </div>
          <span
            v-if="!props.dataset.licenses || props.dataset.licenses.length === 0"
            class="meta-empty"
          >-</span>
        </div>
      </div>
    </div>

    <!-- Column 3: Organisation -->
    <div class="metadata-col">
      <div class="metadata-col-header">
        <SvgIcon
          icon="Building"
          class="col-icon"
        />
        <span class="col-title">{{ t('message.dataset_detail.publisher') }}</span>
      </div>
      <div class="col-content">
        <div class="publisher-wrapper">
          <a
            v-if="props.dataset.publisher?.resource"
            :href="props.dataset.publisher.resource"
            target="_blank"
            class="publisher-link"
          >
            {{ props.dataset.publisher.name }}
            <SvgIcon
              icon="External"
              class="external-icon"
            />
          </a>
          <span
            v-else-if="props.dataset.publisher?.name"
            class="publisher-text"
          >
            {{ props.dataset.publisher.name }}
          </span>
          <span
            v-else
            class="meta-empty"
          >-</span>
        </div>
      </div>
    </div>

    <!-- Column 4: Kategorien -->
    <div class="metadata-col">
      <div class="metadata-col-header">
        <SvgIcon
          icon="Tag"
          class="col-icon"
        />
        <span class="col-title">{{ t('message.dataset_detail.categories') }}</span>
      </div>
      <div class="col-content">
        <div class="categories-wrapper">
          <div
            v-if="props.dataset.getCategoriesForLanguage(locale).length > 0"
            class="category-tags"
          >
            <OdsTagItem
              v-for="category in props.dataset.getCategoriesForLanguage(locale)"
              :key="category.id"
              :id="category.id"
              :label="category.label"
              size="sm"
              variant="default"
            />
          </div>
          <span
            v-else
            class="meta-empty"
          >-</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useI18n } from '#imports'

import OdsTermsOfUseIcon from './OdsTermsOfUseIcon.vue'
import OdsRelativeDateToggle from '../OdsRelativeDateToggle.vue'
import OdsTagItem from '../OdsTagItem.vue'
import SvgIcon from '../SvgIcon.vue'
import type { DcatApChV2DatasetAdapter } from './model/dcat-ap-ch-v2-dataset-adapter.js'

const { locale, t } = useI18n()

interface Props {
  dataset: DcatApChV2DatasetAdapter | undefined
}

const props = defineProps<Props>()
</script>

<style lang="scss" scoped>
.metadata-header {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 24px;
  width: 100%;
  //padding: 24px;
  // background-color: var(--color-secondary-10, rgba(0, 150, 136, 0.03));
 // border: 1px solid var(--color-secondary-100, #dfe4e9);
 // border-radius: 12px;
 // box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05), 0 2px 4px -1px rgba(0, 0, 0, 0.03);
 // margin-bottom: 24px;

  @media (max-width: 768px) {
    grid-template-columns: 1fr;
    gap: 20px;
    padding: 16px;
  }
}

.metadata-col {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.metadata-col-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding-bottom: 8px;
  border-bottom: 1px dashed var(--color-secondary-200, #acb4bd);
}

.col-icon {
  width: 18px;
  height: 18px;
  color: var(--color-primary-500, #009688);
}

.col-title {
  font-size: 0.9rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--color-secondary-400, #596978);
}

.col-content {
  display: flex;
  flex-direction: column;
  gap: 8px;
  font-size: 0.95rem;
  color: var(--color-secondary-700, #2D2D2D);
}

.meta-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.meta-label {
  font-size: 0.8rem;
  color: var(--color-secondary-300, #828e9a);
  font-weight: 500;
}

.frequency-text {
  font-weight: 500;
}

.license-wrapper {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.license-icon {
  height: 48px;
  transition: transform 0.2s ease, opacity 0.2s ease;

  &:hover {
    transform: scale(1.05);
    opacity: 0.9;
  }
}

.publisher-wrapper {
  word-break: break-word;
}

.publisher-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: var(--color-primary-500, #009688);
  font-weight: 500;
  text-decoration: none;
  border-bottom: 1px solid transparent;
  transition: border-bottom-color 0.2s ease, color 0.2s ease;

  &:hover {
    color: var(--color-primary-800, #004a43);
    border-bottom-color: var(--color-primary-800, #004a43);

    .external-icon {
      transform: translate(1px, -1px);
    }
  }
}

.external-icon {
  width: 14px;
  height: 14px;
  transition: transform 0.2s ease;
}

.publisher-text {
  font-weight: 500;
}

.categories-wrapper {
  .category-tags {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
  }
}

.meta-empty {
  color: var(--color-secondary-300, #828e9a);
  font-style: italic;
}
</style>
