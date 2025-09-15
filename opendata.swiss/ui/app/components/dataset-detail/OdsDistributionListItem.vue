<template>
   <div class="download-item">
    <!-- spacer if no download URLs -->
    <SvgIcon v-if="!props.item.downloadUrls.length" title="No download available. This is probably an api. Check details" icon="Download" size="xl" class="download-item__icon disabled" />

    <a v-for="downloadUrl in props.item.downloadUrls" :key="downloadUrl" :href="downloadUrl" target="_blank" rel="noopener" :title="'Download ' + props.item.title" class="download-item__link">
      <SvgIcon icon="Download" size="xl" class="download-item__icon" />
    </a>

    <NuxtLink :to="`${props.datasetId}/distribution/${props.item.id}`" class="no-underline" style="flex-grow: 1;">
      <h3 class="download-item__title">{{ props.item.title }}</h3>
      <p class="download-item__description" />

      <p class="meta-info download-item__meta-info">
        <span v-if="item.format" class="meta-info__item">{{ item.format }}</span>
        <span v-if="item.issued" class="meta-info__item">{{ item.modified ? item.modified : item.issued }}</span>
        <span v-if="item.byteSize" class="meta-info__item">{{ item.byteSize }} Bytes</span>
      </p>
      <div style="display: flex; justify-content: flex-end; align-items: center; height: 100%; margin-top: -10px; margin-top:-40px;">
          <OdsButton icon="ArrowRight" variant="bare" size="sm" class="download-item__button" icon-right :to="`${props.datasetId}/distribution/${props.item.id}`" >
           <span>{{ t('message.dataset_detail.details') }}</span>
          </OdsButton>
      </div>

    </NuxtLink>


  </div>
</template>

<script setup lang="ts">
import SvgIcon from '../SvgIcon.vue';
import OdsButton from '../OdsButton.vue';
import { useI18n } from 'vue-i18n';

const {  t } = useI18n();

const props = defineProps({
  item: {
    type: Object as PropType<DistributionListItem>,
    required: true
  },
  datasetId: {
    type: String,
    required: true
  }
})

export interface DistributionListItem {
  id: string,
  title: string,
  description: string,
  format: string,
  modified: string,
  created: string,
  issued: string,
  accessUrls: string[],
  downloadUrls: string[],
  languages: string[],
  byteSize: string
}

</script>


<style scoped lang="scss">
.disabled {
  fill: lightgray;
  stroke: lightgray;
  color: lightgray;
}
.no-underline {
  text-decoration: none;
}

</style>
