<template>
    <NuxtLink
    v-if="isExternal"
    :href="props.href"
    target="_blank"
    rel="noopener noreferrer"
    class="link--external">
    <slot />
  </NuxtLink>

  <NuxtLinkLocale
    v-else
    :href="props.href"
    :target="props.target"
    :class="{ 'link--external': isExternal }"
  >
    <slot />
  </NuxtLinkLocale>

</template>

<script setup lang="ts">

import type { PropType } from 'vue'
import isAbsolute from 'is-absolute-url';

const props = defineProps({
  href: {
    type: String,
    default: ''
  },
  target: {
    type: String as PropType<'_blank' | '_parent' | '_self' | '_top' | (string & object) | null | undefined>,
    default: undefined,
    required: false
  }
})

const isExternal = computed(() => {
  if (!props.href) {
    return false
  }

  if(props.href.startsWith('mailto')) {
    return false
  }

  if(isAbsolute(props.href)) {
    const base = import.meta.client ? window.location : useRequestURL()

    return new URL(props.href).host !== base.host;
  }

  return false
});
</script>
