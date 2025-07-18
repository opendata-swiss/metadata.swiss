<template>
  <nav class="top-bar-navigation" aria-label="Top bar">
    <ul>
      <li>
        <a href="#">
          <span>Anmelden </span>
          <SvgIcon icon="User" size="lg" />
        </a>
      </li>
    </ul>
  </nav>
</template>

<script setup lang="ts">
import SvgIcon from '../components/SvgIcon.vue'

defineProps({
  context: {
    type: String,
    default: () => undefined,
  },
})
</script>
