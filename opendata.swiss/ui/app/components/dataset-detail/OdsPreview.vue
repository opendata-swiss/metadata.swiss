<template>
  <!-- Logic for later -->
  <!-- <section class="section" v-if="['CSV', 'ODS', 'XLSX', 'XLS'].includes(distribution.format)"> -->
  <section>
    <div class="container container--grid ">
      <div class="container__preview container__main vertical-spacing">
        <h2 class="h2">
          Preview
        </h2>
        <DistributionVisualisation
          download-url="https://www.data.gouv.fr/api/1/datasets/r/860eac55-acd1-4811-98ad-9af4fd361658"
          file-format="csv"
          title="Data Preview"
          :show-ai-tab="false"
          primaryColor="var(--color-primary-600)"
          api-base-url="https://data.europa.eu/api/hub/preview/dataPreview"
          buttonBorderRadius="0px"
        />
      </div>
    </div>
  </section>
  <!-- </section> -->
</template>

<script setup>
import { DistributionVisualisation } from 'piveau-preview-plugin'
</script>

<style scoped>
@media (min-width: 1024px) {
  .container__preview {
    font-variant-east-asian: jis90;
    grid-column: 2 / -2;
    width: 100%;
  }
}
</style>
