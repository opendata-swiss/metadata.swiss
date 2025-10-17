<template>
<ul class="list list--flex list--wrap">
  <li v-for="c in props.tags" :key="c.id">
    <OdsTagItem
      :id="c.id"
      :label="c.label"
      :icon="c.icon"
      :size="c.size"
      :variant="c.variant"
    />
  </li>
</ul>
</template>

<script setup lang="ts">
import type { TagItem } from '../OdsTagItem.vue';
import OdsTagItem from '../OdsTagItem.vue';

interface Props {
  tags: TagItem[];
}

const props = defineProps<Props>();

</script>


