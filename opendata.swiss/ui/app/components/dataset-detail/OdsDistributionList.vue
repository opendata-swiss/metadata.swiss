<template>
  <div class="ods-distribution-list">
    <OdsDistributionListItem v-for="dist in props.distributions" :key="dist.id" :distribution="dist" />
  </div>
</template>

<script setup lang="ts">

import OdsDistributionListItem from './OdsDistributionListItem.vue';
import type { DcatApChV2DistributionAdapter } from './model/dcat-ap-ch-v2-distribution-adapter';


const props = defineProps({
  distributions: {
    type: Array as PropType<DcatApChV2DistributionAdapter[]>,
    required: true
  }
})

</script>


<style lang="scss" scoped>

.ods-distribution-list {
  display: flex;
  flex-direction: column;
}

</style>
