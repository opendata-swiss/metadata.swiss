<template>
  <section :class="`section bg--secondary-${accentColor} color--text-${textColor}`">
    <div class="container">
      <h2
        v-if="title"
        class="section__title"
      >
        <slot name="title">
          {{ title }}
        </slot>
      </h2>
      <div :class="`grid ${layout} gap--responsive`">
        <slot />
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
export type SectionLayout = 'grid--items-1' | 'grid--items-2' | 'grid--items-3' | 'grid--items-4' | 'grid--items-5' | 'grid--responsive-cols-2' | 'grid--responsive-cols-3' | 'grid--responsive-cols-4'

const { accentColor = '200', textColor = '400', layout = 'grid--responsive-cols-3' } = defineProps<{
  title?: string
  layout?: SectionLayout
  accentColor?: '50' | '100' | '200' | '300' | '400' | '500' | '600' | '700' | '800' | '900'
  textColor?: '50' | '100' | '200' | '300' | '400' | '500' | '600' | '700' | '800' | '900'
}>()
</script>
