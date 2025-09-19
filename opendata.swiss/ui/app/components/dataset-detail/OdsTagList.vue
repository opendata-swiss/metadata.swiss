<template>
<ul class="list list--flex list--wrap">
  <li v-for="c in props.tags" :key="c.resource">
    <OdsKeywordTag :link="c.resource" :label="c.label.en ?? ''" />
  </li>
</ul>
</template>

<script setup lang="ts">

import OdsKeywordTag from '@/components/dataset-detail/OdsKeywordTag.vue'

import type { Category } from '@piveau/sdk-core';
const props = defineProps({
  tags: {
    type: Array as PropType<Category[]>,
    required: true
  }
});

</script>
