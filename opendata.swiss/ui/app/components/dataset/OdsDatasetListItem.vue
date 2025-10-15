<template>
  <div class="card card--list card--clickable">
    <div class="card__content">
      <div class="card__body">
        <p class="meta-info">
          <span class="meta-info__item">{{ t('message.dataset_detail.dataset') }}</span>
          <span class="meta-info__item">{{ props.dataset.publisher?.name }}</span>
          <span v-if="props.dataset.releaseDate" class="meta-info__item"><NuxtTime :datetime="props.dataset.releaseDate" :locale="locale"  /></span>
          <span v-if="props.dataset.modificationDate" class="meta-info__item"><NuxtTime :datetime="props.dataset.modificationDate" :locale="locale"  /></span>
        </p>
        <div class="card__title">
          <h3>{{ props.dataset.title }}</h3>
        </div>
         <p>{{ props.dataset.description }}</p>
      </div>
      <div class="card__footer">
        <div class="card__footer__info">
          <p class="meta-info">
            <span class="meta-info__item" style="color:magenta">{{ props.dataset.getCategoriesForLanguage(locale).map(k => k.label).join(', ') }}</span>
            <span class="meta-info__item">{{ props.dataset.keywords.map(k => k.label).join(', ') }}</span>
            <span class="meta-info__item" style="color:red">{{(props.dataset.getOdsFormats ?? []).map(f => f.label ).join(', ') }}</span>
          </p>
        </div>
        <div class="card__footer__action">
          <NuxtLinkLocale :to="{ name: 'datasets-datasetId', params: { datasetId: props.dataset.id }, query: searchParams }" type="false" class="btn btn--outline btn--icon-only" aria-label="false">
            <svg viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg" class="icon icon--base icon--ArrowRight btn__icon">
              <path xmlns="http://www.w3.org/2000/svg" d="m16.444 19.204 4.066-7.044-4.066-7.044-.65.375 3.633 6.294h-15.187v.75h15.187l-3.633 6.294z" />
            </svg>
            <span class="btn__text">Weiterlesen</span>
          </NuxtLinkLocale>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n';
import type { DcatApChV2DatasetAdapter } from '../dataset-detail/model/dcat-ap-ch-v2-dataset-adapter';
import type { LocationQueryRaw } from 'vue-router';

const { t, locale } = useI18n();

const props = defineProps({
  dataset: {
    type: Object as PropType<DcatApChV2DatasetAdapter>,
    required: true,
  },
  searchParams: {
    type: Object as PropType<LocationQueryRaw>,
    required: false,
    default: () => undefined
  }
})

</script>
