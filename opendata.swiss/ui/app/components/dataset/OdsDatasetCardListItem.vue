<template>
  <OdsCard :title="props.item.getTitle" type="universal" clickable>
    <template #top-meta>
      <div>
        <span class="meta-info__item">{{ t('message.dataset_detail.dataset') }}</span>
      </div>
    </template>
    <p>{{ item.getDescription }} </p>
    <template #bottom-meta>
      <span class="meta-info__item">{{ props.item.getPublisher?.name }}</span>
      <span v-if="props.item.getIssued" class="meta-info__item"><NuxtTime :datetime="props.item.getIssued" :locale="locale"  /></span>
      <span v-if="props.item.getModified" class="meta-info__item"><NuxtTime :datetime="props.item.getModified" :locale="locale"  /></span>
    </template>
    <template #icons>
      <SvgIcon icon="Youtube" size="xl" />
      <SvgIcon icon="EasyLanguage" size="xl" />
      <SvgIcon icon="SignLanguage" size="xl" />
    </template>
    <template #footer-info>
      <span class="meta-info__item" style="color:magenta">{{ props.item.getCategories.map(k => k.label['en']).join(', ') }}</span>
      <span class="meta-info__item">{{ props.item.getKeywords.map(k => k.label).join(', ') }}</span>
      <span class="meta-info__item" style="color:red">{{(props.item.getOdsFormats ?? []).map(f => f.label ).join(', ') }}</span>
    </template>
    <template #footer-action>
      <NuxtLinkLocale :to="{ name: 'datasets-datasetId', params: { datasetId: props.item.getId }, query: searchParams }" type="false" class="btn btn--outline btn--icon-only" aria-label="false">
        <SvgIcon icon="ArrowRight" role="btn" />
        <span class="btn__text">Weiterlesen</span>
      </NuxtLinkLocale>
    </template>
  </OdsCard>

</template>

<script setup lang="ts">
import type { Dataset } from '~/model/dataset';

import { useI18n } from 'vue-i18n'

const { t, locale } = useI18n()
interface Props {
  item: Dataset
  searchParams?: Record<string, string>
}

const props = defineProps<Props>()


</script>
