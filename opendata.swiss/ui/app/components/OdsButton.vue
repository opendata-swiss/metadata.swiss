<template>
  <button
    :type="!submit ? 'button' : undefined"
    :class="['btn', 'btn--base', classes]"
    :aria-label="title"
    :title="title"
  >
    <slot name="icon">
      <a
        v-if="href && icon"
        :href="localePath(href)"
        :target="linkTarget"
      >
        <SvgIcon
          v-if="icon"
          :icon="icon"
          :size="size"
          class="btn__icon"
        />
      </a>
      <SvgIcon
        v-else-if="icon"
        :icon="icon"
        :size="size"
        class="btn__icon"
      />
    </slot>
    <span class="btn__text">
      <a
        v-if="href"
        :href="localePath(href)"
        :target="linkTarget"
      >
        <slot>{{ title }}</slot>
      </a>
      <slot v-else>{{ title }}</slot>
    </span>
  </button>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import SvgIcon from './SvgIcon.vue'

const localePath = useLocalePath()

const { title, iconOnly = false, ...props } = defineProps<{
  title?: string
  variant?: 'outline' | 'bare' | 'filled' | 'outline-negative' | 'bare-negative' | 'link' | 'link-negative'
  size?: 'sm' | 'md' | 'lg'
  iconOnly?: boolean
  iconRight?: boolean
  icon?: string
  href?: string
  submit?: boolean
  target?: '_blank' | '_self'
}>()

const classes = computed(() => {
  const classes = []

  if (iconOnly) {
    classes.push('btn--icon-only')
  }

  if (props.size) {
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
const linkTarget = computed(() => {
  return props.target ?? '_self'
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
a {
  text-decoration: none;
}
/* end fixes */
</style>
