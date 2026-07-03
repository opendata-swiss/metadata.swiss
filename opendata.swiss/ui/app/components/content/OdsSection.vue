<template>
  <section :class="sectionClasses">
    <div class="container">
      <h2
        v-if="title"
        class="section__title"
      >
        <slot name="title">
          {{ title }}
        </slot>
      </h2>
      <div
        v-if="layout === 'slideshow'"
        class="carousel carousel--cards"
      >
        <Swiper
          :speed="slideshowOptions?.speed"
          :auto-height="false"
          :loop="false"
          :slides-per-view="1"
          :space-between="20"
          :autoplay="slideshowOptions?.autoplay"
          :breakpoints="breakpoints"
          :modules="slideshowModules"
          :keyboard="{
            enabled: true,
            onlyInViewport: false,
          }"
          :navigation="{
            nextEl: `#carousel-next-${id}`,
            prevEl: `#carousel-prev-${id}`,
          }"
          :simulate-touch="true"
          :slide-to-clicked-slide="false"
          :pagination="{
            type: 'bullets',
            el: `#carousel-pagination-${id}`,
            clickable: true,
            bulletClass: 'carousel__bullet',
            bulletActiveClass: 'carousel__bullet--active',
          }"
        >
          <slot />
        </Swiper>
        <div class="carousel__fonctions">
          <div
            :id="`carousel-pagination-${id}`"
            class="carousel__pagination"
          />
          <button
            :id="`carousel-prev-${id}`"
            class="carousel__prev"
          >
            <div class="sr-only">
              Previous image
            </div>
            <SvgIcon
              icon="ChevronLeft"
              role="presentation"
              aria-hidden="true"
            />
          </button>
          <button
            :id="`carousel-next-${id}`"
            class="carousel__next"
          >
            <div class="sr-only">
              Next image
            </div>
            <SvgIcon
              icon="ChevronRight"
              role="presentation"
              aria-hidden="true"
            />
          </button>
        </div>
      </div>
      <div
        v-else
        :class="`grid ${layout} gap--responsive`"
      >
        <slot />
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import type { SwiperOptions } from 'swiper/types'
import { Navigation, Pagination, A11y, Autoplay } from 'swiper/modules'

export type SectionLayout = 'grid--items-1' | 'grid--items-2' | 'grid--items-3' | 'grid--items-4' | 'grid--items-5' | 'grid--responsive-cols-2' | 'grid--responsive-cols-3' | 'grid--responsive-cols-4' | 'slideshow'

const {
  id = Math.ceil(Math.random() * 10),
  accentColor = '200',
  textColor = '400',
  layout = 'grid--responsive-cols-3',
  slideshowOptions,
} = defineProps<{
  id?: number
  title?: string
  layout?: SectionLayout
  accentColor?: '50' | '100' | '200' | '300' | '400' | '500' | '600' | '700' | '800' | '900'
  textColor?: '50' | '100' | '200' | '300' | '400' | '500' | '600' | '700' | '800' | '900'
  slideshowOptions?: SwiperOptions
}>()

const sectionClasses = computed(() => {
  if (layout === 'slideshow') {
    return 'section section-default'
  }

  return `section section--default accent-${accentColor} text-${textColor}`
})

const breakpoints = {
  480: {
    slidesPerView: 1,
    spaceBetween: 28,
  },
  640: {
    slidesPerView: 1,
    spaceBetween: 36,
  },
  768: {
    slidesPerView: 2,
    slidesPerGroup: 2,
    spaceBetween: 36,
  },
  1024: {
    slidesPerView: 3,
    slidesPerGroup: 3,
    spaceBetween: 40,
  },
  1280: {
    slidesPerView: 3,
    slidesPerGroup: 3,
    spaceBetween: 48,
  },
  1800: {
    slidesPerView: 3,
    slidesPerGroup: 3,
    spaceBetween: 64,
  },
}

const slideshowModules = computed(() => {
  const coreModules = [Navigation, Pagination, A11y]

  if (slideshowOptions?.autoplay?.enabled) {
    return [...coreModules, Autoplay]
  }

  return coreModules
})
</script>
