<script setup lang="ts">
import { SwiperSlide } from 'swiper/vue'
import type CardProps from '~/components/CardProps'
import Card from '../CardBase.vue'

const { slideshowCard, ...props } = defineProps<CardProps & {
  slideshowCard?: boolean
}>()
</script>

<template>
  <template v-if="slideshowCard">
    <SwiperSlide>
      <Card v-bind="props" />
    </SwiperSlide>
  </template>
  <Card
    v-else
    v-bind="props"
  />
</template>
