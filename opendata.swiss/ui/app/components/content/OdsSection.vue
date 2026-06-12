<template>
  <section class="section bg--secondary-50">
    <div class="container">
      <h2 class="section__title">
        <slot name="title">
          {{ title }}
        </slot>
      </h2>
      <div class="grid grid--items-5 gap--responsive">
        <slot />
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
defineProps<{
  title: string
}>()
</script>
