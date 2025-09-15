<template>
  <button
  type="button"
  :class="['btn', 'btn--base', classes]"
  :aria-label="title"
  :title="title"
  >
    <slot name="icon">
      <SvgIcon v-if="icon" :icon="icon" :size="size" class="btn__icon" />
    </slot>
    <span class="btn__text">
      <a v-if="href" :href="href" >
        <slot>{{ title }}</slot>
      </a>
      <slot v-else>{{ title }}</slot>
    </span>

  </button>
</template>

<script setup lang="ts">
import { computed } from 'vue';

const { title, iconOnly = false, ...props } = defineProps<{
  title?: string
  variant?: 'outline' | 'bare' | 'filled' | 'outline-negative' | 'bare-negative' | 'link' | 'link-negative'
  size?: 'sm' | 'md' | 'lg'
  iconOnly?: boolean
  iconRight?: boolean
  icon?: string
  href?: string
}>()

const classes = computed(() => {
  const classes = []

  if(iconOnly) {
    classes.push('btn--icon-only')
  }

  if(props.size) {
    classes.push(`btn--${props.size}`)
  }

  if (props.variant) {
    classes.push(`btn--${props.variant}`)
  }

  if (props.iconRight) {
    classes.push('btn--icon-right')
  }
  else if (props.icon) {
    classes.push('btn--icon-left')
  }

  return classes
})
</script>

<style scoped>
/* Fixes styles being overridden. I could not figure out where */
.btn--outline {
  color: var(--color-primary-600) !important;
}

.btn--outline-negative, .btn--outline-negative a {
  color: rgb(255 255 255 / var(--tw-text-opacity, 1));
}
/* end fixes */
</style>
