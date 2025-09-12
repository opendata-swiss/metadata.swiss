<template>
<div>
  <template v-for="row in tableEntries" :key="row.id">
    <OdsInfoBlock :title="row.label">
      <div v-if="row.type === 'value'">{{ row.value }}</div>
      <div v-if="row.type === 'href'">
        <a v-if="row.href && row.href.startsWith('http')" :href="row.href" class="link--external">{{ row.value }}</a>
        <a v-if="row.href && !row.href.startsWith('http')" :href="row.href">{{ row.value }}</a>
      </div>
    </OdsInfoBlock>
  </template>
</div>


</template>

<script setup lang="ts">

import type { PropertyTableEntry, PropertyTableEntryNode } from '@piveau/sdk-vue';
import { computed } from 'vue'
import OdsInfoBlock from "~/components/OdsInfoBlock.vue";

interface TableEntry {
  id: string;
  label: string;
  value: string;
  href?: string;
  type: 'value' | 'href';
}

interface Props {
  rootNode: PropertyTableEntryNode
}

const props = defineProps<Props>()





const tableEntries = computed(() => {
//  const nodeTree = definePropertyNode({ id: 'root', data: resultEnhanced.value?.getPropertyTable }, { compact: true, maxDepth: 5 })
  const flattenedNodes = flattenNode(props.rootNode);
  return flattenedNodes;
})

function flattenNode(node: PropertyTableEntry): TableEntry[] {
  if (node.type === 'node') {
    const valueNodes = node.data?.filter(n => n.type === 'value') ?? [];
    const hrefNodes = node.data?.filter(n => n.type === 'href') ?? [];
    const nestedNodes = node.data?.filter(n => n.type === 'node') ?? [];

    const valueEntries: TableEntry[] = valueNodes.map(n => ({
      id: n.id,
      label: node.label || '',
      value: n.data || '',
      type: 'value'
    }));

    const hrefEntries: TableEntry[] = hrefNodes.map(n => ({
      id: n.id,
      label: node.label || '',
      value: n.data.label || '',
      href: n.data.href || '',
      type: 'href'
    }));
    const nestedEntries: TableEntry[] = nestedNodes.flatMap(n => flattenNode(n));
    return [...valueEntries, ...hrefEntries, ...nestedEntries];
  }

  return [];

}
</script>

<style lang="scss">

</style>
