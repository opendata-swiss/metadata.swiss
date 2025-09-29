<template>
  <ul class="download-items">
    <li v-for="url in props.downloadUrls" :key="url">
      <OdsDownloadListItem :download-url="url" :name="props.name" :format="props.format" :languages="props.languages" :byte-size="props.byteSize" />
    </li>
  </ul>
</template>


<script setup lang="ts">
import OdsDownloadListItem from './OdsDownloadListItem.vue';

const props = defineProps({
  downloadUrls: {
    type: Array as PropType<string[]>,
    required: true
  },
  format : {
    type: String,
    required: false,
    default: ''
  },
  name : {
    type: String,
    required: true
  },
  languages: {
    type: Array as PropType<string[]>,
    required: false,
    default: () => []
  },
  byteSize: {
    type: String,
    required: false,
    default: ''
  }
});

</script>
