<template>
  <OdsCard :title="props.dataset.title ?? ''" type="universal" clickable class="strech-card">
    <template #top-meta>
      <div>
        <span class="meta-info__item">{{ t('message.dataset_detail.dataset') }}</span>
      </div>
    </template>
    <p>{{ props.dataset.description }} </p>
    <template #bottom-meta>
      <span class="meta-info__item">{{ props.dataset.publisher?.name }}</span>
      <span v-if="props.dataset.releaseDate" class="meta-info__item"><NuxtTime :datetime="props.dataset.releaseDate" :locale="locale"  /></span>
      <span v-if="props.dataset.modificationDate" class="meta-info__item"><NuxtTime :datetime="props.dataset.modificationDate" :locale="locale"  /></span>
    </template>
    <template #icons>
      <SvgIcon icon="Youtube" size="xl" />
      <SvgIcon icon="EasyLanguage" size="xl" />
      <SvgIcon icon="SignLanguage" size="xl" />
    </template>
    <template #footer-info>
      <span class="meta-info__item" style="color:magenta">{{ props.dataset.getCategoriesForLanguage(locale).map(k => k.label).join(', ') }}</span>
      <span class="meta-info__item">{{ props.dataset.keywords.map(k => k.label).join(', ') }}</span>
      <span class="meta-info__item" style="color:red">{{(props.dataset.getOdsFormats ?? []).map(f => f.label).join(', ') }}</span>
    </template>
    <template #footer-action>
      <NuxtLinkLocale :to="{ name: 'datasets-datasetId', params: { datasetId: props.dataset.id }, query: searchParams }" type="false" class="btn btn--outline btn--icon-only" aria-label="false">
        <SvgIcon icon="ArrowRight" role="btn" />
        <span class="btn__text">Weiterlesen</span>
      </NuxtLinkLocale>
    </template>
  </OdsCard>
</template>

<script setup lang="ts">
import { useI18n } from '#imports'
import OdsCard from '~/components/OdsCard.vue'
import SvgIcon from '~/components/SvgIcon.vue'
import type { DcatApChV2DatasetAdapter } from '../dataset-detail/model/dcat-ap-ch-v2-dataset-adapter'
import type { LocationQueryRaw } from 'vue-router'

const { t, locale } = useI18n()

interface Props {
  dataset: DcatApChV2DatasetAdapter
  searchParams?: LocationQueryRaw
}

const props = defineProps<Props>()
</script>

<style scoped lang="scss">
.strech-card {
  height: 100%;
}
</style>
