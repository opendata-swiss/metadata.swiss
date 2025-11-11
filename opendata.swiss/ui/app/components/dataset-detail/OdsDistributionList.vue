<template>
  <div class="ods-distribution-list">
    <OdsDistributionListItem v-for="dist in props.distributions" :key="dist.id" :distribution="dist" />
  </div>
</template>

<script setup lang="ts">

import OdsDistributionListItem from './OdsDistributionListItem.vue';
import type { DcatApChV2DistributionAdapter } from './model/dcat-ap-ch-v2-distribution-adapter';

interface Props {
  distributions: DcatApChV2DistributionAdapter[];
}

const props = defineProps<Props>();

</script>


<style lang="scss" scoped>

.ods-distribution-list {
  display: flex;
  flex-direction: column;
}

</style>
