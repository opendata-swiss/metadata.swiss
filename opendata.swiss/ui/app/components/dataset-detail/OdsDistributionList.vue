<template>
  <OdsDistributionListItem v-for="item in items" :key="item.id" :item="item" :dataset-id="props.dataset.getId" />
</template>

<script setup lang="ts">

import { computed } from 'vue'

import { useI18n } from 'vue-i18n'

import type { Dataset } from '~/model/dataset';
import OdsDistributionListItem, { type DistributionListItem } from './OdsDistributionListItem.vue';
import type { PropertyTableEntryBase, PropertyTableEntryNode } from '@piveau/sdk-vue';

const { locale } = useI18n()


const props = defineProps({
  dataset: {
    type: Object as PropType<Dataset>,
    required: true
  }
})

const items = computed<DistributionListItem[]>(() => {
  const distributions = props.dataset.getDistributions
  return (distributions || []).map(d => {
    const propertyTable = d.getPropertyTable ?? []
    const byteSize = findByteSize(propertyTable)
    return {
    id: d.id ?? '',
    title: d.title ?? '',
    description: d.description ?? '',
    format: d.format ?? '',
    modified: d.modified ?? '',
    created: d.created ?? '',
    issued: d.issued ?? '',
    accessUrls: d.accessUrls || [],
    downloadUrls: d.downloadUrls || [],
    languages: d.languages || [],
    byteSize
  }}).sort((a, b) => a.title.localeCompare(b.title, locale.value))
})

function findByteSize(table: PropertyTableEntryNode[] | undefined): string {
  const byteSizeNode = table?.find(entry => entry.id === 'byteSize')
  if(!byteSizeNode) {
    return ''
  }
  const valueEntry = byteSizeNode.data?.find((entry: PropertyTableEntryBase) => entry.id === 'byteSize_value')
  if(!valueEntry) {
    return ''
  }
  return valueEntry.data as string
}

</script>
