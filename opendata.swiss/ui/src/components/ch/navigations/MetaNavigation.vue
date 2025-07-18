<template>
  <div class="meta-navigation-container">
    <nav class="meta-navigation meta-navigation--desktop" aria-label="Meta">
      <ul>
        <li>
          <a href="#" class="meta-navigation__item"> mata nav Jobs </a>
        </li>
        <li>
          <a href="#" class="meta-navigation__item">  mata nav  Kontakt </a>
        </li>
        <li>
          <a href="#" class="meta-navigation__item">  mata nav  Medien </a>
        </li>
      </ul>
    </nav>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'

const screenSize = ref(0)

defineProps({
  isFreebrand: {
    type: Boolean,
    default: () => false,
  },
})

const resizeWindow = function () {
  screenSize.value = document.body.clientWidth
}

onMounted(() => {
  resizeWindow()
  window.addEventListener('resize', resizeWindow)
})
</script>
