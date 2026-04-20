<template>
  <div
    v-if="props.type == 'block'"
    class="info-block odsTable border-t"
  >
    <h3 class="info-block__title">
      {{ title }}
    </h3>
    <div>
      <slot />
    </div>
  </div>
  <div
    v-if="props.type === 'row'"
    class="odsTableRow"
  >
    <h3 class="info-block__title">
      {{ title }}
    </h3>
    <div>
      <slot />
    </div>
  </div>
  <div
    v-if="props.type === 'tree'"
    class="info-block odsTable"
  >
    <h3 class="info-block__title">
      {{ title }}
    </h3>
    <div>
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
interface OdsInfoBlockProps {
  title: string
  type: 'row' | 'block' | 'tree'
}

const props = defineProps<OdsInfoBlockProps>()
</script>

<style lang="scss">
  .odsTable {
    display: flex;
    flex-direction: column;
    margin-top: 0;
  }
  .odsTableRow {
    display: grid;
    grid-template-columns: 180px 1fr;
    align-items: center;
    column-gap: 1rem;
    .info-block__title {
      margin-bottom: 0;
      font-weight: 600;
      color: grey;
    }
  }
</style>
