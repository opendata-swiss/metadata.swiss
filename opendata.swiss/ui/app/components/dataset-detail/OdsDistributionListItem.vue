<template>
  <div class="download-item">
    <!-- spacer if no download URLs -->
    <SvgIcon v-if="!props.distribution.downloadUrls.length" title="No download available. This is probably an api. Check details" icon="Download" size="xl" class="download-item__icon disabled" />

    <a v-for="downloadUrl in props.distribution.downloadUrls" :key="downloadUrl" :href="downloadUrl" target="_blank" rel="noopener" :title="'Download ' + props.distribution.title" class="download-item__link">
      <SvgIcon icon="Download" size="xl" class="download-item__icon" />
    </a>

    <NuxtLink :to="`${props.distribution.dataset.id}/distribution/${props.distribution.id}`" class="no-underline no-overvlow">
      <h3 class="download-item__title" :title="props.distribution.title">{{ props.distribution.title }}</h3>
      <p class="download-item__description" />

      <div class="footer">
        <p class="meta-info download-item__meta-info">
          <span v-if="props.distribution.format" class="meta-info__item">{{ props.distribution.format }}</span>
          <span v-if="props.distribution.releaseDate" class="meta-info__item">
            <OdsRelativeDateToggle :date="props.distribution.modified ? props.distribution.modified : props.distribution.releaseDate" />
          </span>
          <span v-if="props.distribution.formattedByteSize" class="meta-info__item">{{ props.distribution.formattedByteSize }}</span>
        </p>
        <OdsButton icon="ArrowRight" variant="bare" size="sm" class="download-item__button" icon-right :to="`${props.distribution.dataset.id}/distribution/${props.distribution.id}`" >
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
import type { DcatApChV2DistributionAdapter } from './model/dcat-ap-ch-v2-distribution-adapter';
import OdsRelativeDateToggle from '../OdsRelativeDateToggle.vue';

const { t } = useI18n();

const props = defineProps({
  distribution: {
    type: Object as PropType<DcatApChV2DistributionAdapter>,
    required: true
  }
})

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

.no-overvlow {
  overflow-x: hidden;
  flex-grow: 1;
}

.download-item__title {
  text-overflow: ellipsis;
  overflow: hidden;
  white-space: nowrap;
}

.footer {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
}
</style>
