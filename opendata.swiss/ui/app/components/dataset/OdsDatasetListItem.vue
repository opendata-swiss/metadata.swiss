<template>

<div class="card card--list card--clickable">

   <!---->
   <div class="card__content">
      <div class="card__body">
        <p class="meta-info">
          <span class="meta-info__item">{{ t('message.dataset_detail.dataset') }}</span>
          <span class="meta-info__item">{{ props.item.getPublisher?.name }}</span>
          <span v-if="props.item.getIssued" class="meta-info__item"><NuxtTime :datetime="props.item.getIssued" :locale="locale"  /></span>
          <span v-if="props.item.getModified" class="meta-info__item"><NuxtTime :datetime="props.item.getModified" :locale="locale"  /></span>
        </p>
         <div class="card__title">
            <h3>{{ props.item.getTitle }}</h3>
         </div>
         <p>{{ props.item.getDescription }}</p>
         <!---->
      </div>
      <div class="card__footer">
         <div class="card__footer__info">
          <p class="meta-info">
            <span class="meta-info__item" style="color:magenta">{{ props.item.getCategories.map(k => k.label['en']).join(', ') }}</span>
            <span class="meta-info__item">{{ props.item.getKeywords.map(k => k.label).join(', ') }}</span>
            <span class="meta-info__item" style="color:red">{{(props.item.getOdsFormats ?? []).map(f => f.label ).join(', ') }}</span>
          </p>
         </div>
         <div class="card__footer__action">
              <NuxtLinkLocale :to="{ name: 'datasets-datasetId', params: { datasetId: props.item.getId }, query: searchParams }" type="false" class="btn btn--outline btn--icon-only" aria-label="false">
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
import type { Dataset } from '~/model/dataset';
import { useI18n } from 'vue-i18n';

const { t, locale } = useI18n();

interface Props {
  item: Dataset
  searchParams?: Record<string, string>
}
const props = defineProps<Props>()

</script>
