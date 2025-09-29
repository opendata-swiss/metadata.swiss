<template>
  <div>
    <template v-for="entry in props.tableEntries" :key="entry.id">
      <OdsInfoBlock :title="entry.label">
        <template v-for ="(data, index) in entry.value" :key="index">
            <span v-if="data.type === 'value'">
            <template v-if="data.value">
              <template v-if="!isNaN(Date.parse(data.value))">
              <OdsRelativeDateToggle :date="new Date(data.value)" />
              </template>
              <template v-else>
              {{ data.value }}
              </template>
            </template>
            </span>
          <span v-if="data.type === 'href'">
            <a :href="data.href" target="_blank" rel="noopener noreferrer" class="link--external">{{ data.value }}</a>
          </span>
          <span v-if="data.type === 'email'">
            <a :href="data.href">&lt;{{ data.value }}&gt;</a>
          </span>
          <span>&nbsp;</span>
        </template>
      </OdsInfoBlock>
    </template>
  </div>
</template>

<script setup lang="ts">
import type { TableEntry } from './model/table-entry';
import OdsInfoBlock from '../OdsInfoBlock.vue';
import OdsRelativeDateToggle from '../OdsRelativeDateToggle.vue';

const props = defineProps({
  tableEntries: {
    type: Array as PropType<TableEntry[]>,
    required: true
  }
})

</script>

<style lang="scss">

</style>
