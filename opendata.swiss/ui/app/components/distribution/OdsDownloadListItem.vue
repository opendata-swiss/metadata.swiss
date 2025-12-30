<template>
  <div>
    <a class="download-item" :href="props.downloadUrl" target="_blank">
      <SvgIcon icon="Download" size="xl" class="download-item__icon" />
      <div>
        <h2 class="download-item__title">{{ props.name }}</h2>
        <p class="meta-info download-item__meta-info">
          <span v-if="props.format" class="meta-info__item">{{ props.format }}</span>
          <span v-if="props.byteSize" class="meta-info__item">{{ props.byteSize }}</span>
          <span v-if="props.languages?.length" class="meta-info__item">{{ props.languages.join(', ') }}</span>
        </p>
      </div>
    </a>
  </div>
</template>

<script setup lang="ts">
import SvgIcon from '../SvgIcon.vue'

interface OdsDownloadListItemProps {
  downloadUrl: string
  name: string
  format?: string
  languages?: string[]
  byteSize?: string
}

const props = defineProps<OdsDownloadListItemProps>()
</script>

<style scoped lang="scss">
  a {
    overflow-x: hidden;
    word-break: break-all;
  }

  .download-item__title {
    text-overflow: hidden;
    overflow-x: hidden;
    word-break: break-all;
  }
</style>
