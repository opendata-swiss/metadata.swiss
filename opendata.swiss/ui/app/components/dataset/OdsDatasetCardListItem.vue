<template>
  <OdsCard
    :title="props.dataset.title ?? ''"
    type="universal"
    clickable
    class="strech-card"
  >
    <template #top-meta>
      <div>
        <span class="meta-info__item">{{ t('message.dataset_detail.dataset') }}</span>
        <span
          v-if="props.dataset.releaseDate && !props.dataset.modificationDate"
          class="meta-info__item"
        ><NuxtTime
          :datetime="props.dataset.releaseDate"
          :locale="locale"
        /></span>
        <span
          v-if="props.dataset.modificationDate"
          class="meta-info__item"
        ><NuxtTime
          :datetime="props.dataset.modificationDate"
          :locale="locale"
        /></span>
      </div>
    </template>
    <div>
      <div>
        <span class="meta-info__item">{{ props.dataset.publisher?.name }}</span>
      </div>
    </div>
    <p>{{ props.dataset.description }} </p>

    <template #footer-info>
      <p>
        <span class="meta-info__item">
          <i> {{ props.dataset.getCategoriesForLanguage(locale).map(k => k.label).join(', ') }} </i>
        </span>
      </p>
      <OdsTagList
        v-if="props.dataset.keywords.length > 0"
        :tags="[...props.dataset.formats, ...props.dataset.keywords]"
      />

      <span class="meta-info__item">
        <CommentCount
          :page-id="`dataset-${props.dataset.id}`"
          :language="locale"
        />
      </span>
    </template>
    <template #footer-action>
      <NuxtLinkLocale
        :to="{ name: 'datasets-datasetId', params: { datasetId: props.dataset.id }, query: searchParams }"
        type="false"
        class="btn btn--outline btn--icon-only"
        aria-label="false"
      >
        <SvgIcon
          icon="ArrowRight"
          role="btn"
        />
        <span class="btn__text">Weiterlesen</span>
      </NuxtLinkLocale>
    </template>
  </OdsCard>
</template>

<script setup lang="ts">
import { useI18n } from '#imports'
import OdsCard from '../content/OdsCard.vue'
import SvgIcon from '~/components/SvgIcon.vue'
import type { DcatApChV2DatasetAdapter } from '../dataset-detail/model/dcat-ap-ch-v2-dataset-adapter'
import type { LocationQueryRaw } from 'vue-router'
import { CommentCount } from '@hyvor/hyvor-talk-vue'
import OdsTagList from '../dataset-detail/OdsTagList.vue'

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
