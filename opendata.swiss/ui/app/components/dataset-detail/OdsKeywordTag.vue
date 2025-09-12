<template>
<a :href="props.link" type="false" aria-label="false">
  <OdsTagItem :label="props.label" />
</a>
</template>


<script setup lang="ts">

import OdsTagItem from "~/components/OdsTagItem.vue";

const props = defineProps<{
  link: string
  label: string
}>()
</script>
