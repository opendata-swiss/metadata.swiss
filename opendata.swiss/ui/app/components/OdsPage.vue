<script setup>
import OdsToc from "~/components/OdsToc.vue";

const { page } = defineProps({
  page: {
    type: Object,
    default: () => null
  },
})
</script>

<template>
  <header id="main-header">
    <slot name="header" />
  </header>
  <div id="main-content">
    <section v-if="page" class="hero hero--default">
      <div class="container container--grid gap--responsive">
        <div class="hero__content">
          <h1 class="hero__title">{{ page.heading || page.title }}</h1>
          <div class="hero__description">
            <slot name="hero-subheading">
              <MDC v-if="page.subHeading" :value="page.subHeading"/>
            </slot>
          </div>
        </div>
      </div>
    </section>
    <slot>
      <section class="section section--py">
        <div class="container container--grid container--reverse-mobile gap--responsive">
          <div class="container__main vertical-spacing">
              <ContentRenderer :value="page"/>
          </div>
          <div class="container__aside">
            <div id="aside-content" class="sticky sticky--top">
              <OdsToc v-if="page.body?.toc?.links.length" :toc="page.body.toc"/>
              <slot name="aside-content" />
            </div>
          </div>
        </div>
      </section>
    </slot>
  </div>

</template>
