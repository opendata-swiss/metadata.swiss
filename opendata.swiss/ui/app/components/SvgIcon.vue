<template>
  <span v-if="showPlaceholder" :class="['icon', 'icon--base', ...iconClasses, $attrs.class]" style="aspect-ratio: 1;" />
  <InlineSvg v-if="svgSrc" :src="svgSrc" :class="['icon', 'icon--base', ...iconClasses, $attrs.class]" @loaded="hidePlaceholder" />
</template>

<script setup lang="ts">
import InlineSvg from 'vue-inline-svg'
import { computed, ref, watchEffect } from 'vue'

interface PropTypes {
  icon: string,
  size?: 'sm' | 'base' | 'md' | 'lg' | 'xl' | '2xl' | '3xl' | '4xl' | 'full',
  spin?: boolean,
  role?: 'btn' | 'menu__item' | 'navigation',
}

const props = defineProps<PropTypes>()

const svgSrc = ref('')

const showPlaceholder = ref(true)
const iconClasses = computed(() => {
  function * generate() {
    if (props.size) yield `icon--${props.size}`
    if (props.icon) yield `icon--${props.icon}`
    if (props.spin) yield `icon--spin`
    if (props.role) yield `${props.role}__icon`
  }

  return [...generate()]
})

function hidePlaceholder() {
  showPlaceholder.value = false
}

watchEffect(async () => {
  // TODO: check if this works in production. Not consistent in dev
  if (!props.icon) {
    svgSrc.value = ''
    return
  }
  svgSrc.value = (
    await import(`~/assets/icons/${props.icon}.svg`)
  ).default
})
</script>
