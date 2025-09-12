<template>
  <ul v-if="props.dataset && props.dataset.getDistributions" class="download-items">
    <li v-for="distribution in props.dataset.getDistributions" :key="distribution.id" >
      <a v-for="link in distribution.downloadUrls" :key="link" :href="link"  class="download-item"  target="_blank">
          <SvgIcon icon="Download" size="xl" role="download-item" />
          <div>
            <h2 class="download-item__title">{{ distribution.title }}</h2>
            <!---->
            <p class="meta-info download-item__meta-info"><span class="meta-info__item">{{ distribution.format ? distribution.format : distribution.title }}</span><span class="meta-info__item">{{ distribution.languages.join(', ') }}</span></p>
          </div>
      </a>
<NuxtLink :to="`${props.dataset.getId}/distribution/${distribution.id}`">ocho</NuxtLink>
    </li>
  </ul>
</template>

<script setup lang="ts">
import type { Dataset } from '~/model/dataset';
import SvgIcon from "~/components/SvgIcon.vue";

interface Props {
  dataset: Dataset
}

const props = defineProps<Props>();


</script>
