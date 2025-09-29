<script setup lang="ts">
const { title, type = 'default', clickable = false } = defineProps<{
  title: string
  type?: 'default' | 'highlight' | 'twitter' | 'flat' | 'universal' | 'list'
  clickable?: boolean
}>()

const classes = computed(() => {
  return {
    'card': true,
    [`card--${type}`]: true,
    'card--clickable': clickable,
  }
})
</script>

<template>
  <div :class="classes">
    <div
      v-if="$slots.image && type !== 'highlight' && type !== 'universal'"
      class="card__image"
    >
      <slot name="image" />
    </div>
    <div class="card__content">
      <div class="card__body">
        <p v-if="$slots['top-meta']" class="meta-info">
          <slot name="top-meta" />
        </p>
        <div class="card__title">
          <h3>{{ title }}</h3>
        </div>
        <div v-if="$slots.image && type === 'universal'" class="card__image">
          <slot name="image" />
        </div>
        <slot/>

        <p v-if="$slots['bottom-meta']" class="meta-info">
          <slot name="bottom-meta" />
        </p>
        <div v-if="$slots.icons" class="card__content-icons">
          <slot name="icons"/>
        </div>
      </div>
      <div class="card__footer">
        <div class="card__footer__info">
          <p v-if="$slots['footer-info']" class="meta-info">
            <slot name="footer-info"/>
          </p>
        </div>
        <div v-if="$slots['footer-action']" class="card__footer__action">
          <slot name="footer-action"/>
        </div>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.card {
  height: fit-content;
}
</style>
