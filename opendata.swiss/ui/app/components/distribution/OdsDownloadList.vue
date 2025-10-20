<template>
  <ul class="download-items">
    <li v-for="url in props.downloadUrls" :key="url">
      <OdsDownloadListItem :download-url="url" :name="props.name" :format="props.format" :languages="props.languages" :byte-size="props.byteSize" />
    </li>
  </ul>
</template>


<script setup lang="ts">
import OdsDownloadListItem from './OdsDownloadListItem.vue';

interface OdsDownloadListProps {
  name: string;
  downloadUrls: string[];
  format?: string;
  languages?: string[];
  byteSize?: string;
}

const props = defineProps<OdsDownloadListProps>();

</script>
