<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import LanguageSwitcher from '../components/LanguageSwitcher.vue'
import TopBarNavigation from '../navigations/TopBarNavigation.vue'

const useStickyPlaceholder = ref(false)
const initialTopBarOffset = ref(0)

const props = defineProps({
  isSticky: {
    type: Boolean,
    default: () => false,
  },
})

const computedTopBarClass = computed(() => {
  const base = `top-bar__bar`
  return base
})

const resizeWindow = function () {
  const topBar = document.getElementById('top-bar-container') as HTMLElement
  initialTopBarOffset.value = topBar.offsetTop
  handleScroll()
}

const handleScroll = async function () {
  const topBar = document.getElementById('top-bar') as HTMLElement
  if (window.scrollY > initialTopBarOffset.value) {
    useStickyPlaceholder.value = true
    await nextTick()
    // Set height on placeholder to avoid jump when top bar is set to sticky
    const stickyPlaceholder = document.getElementById(
      'stickyTopBarPlaceholder',
    ) as HTMLElement
    stickyPlaceholder.style.height = `${topBar.clientHeight}px`

    topBar.classList.add('sticky-top-bar')
  } else {
    useStickyPlaceholder.value = false
    topBar.classList.remove('sticky-top-bar')
  }
}

onMounted(() => {
  if (props.isSticky) {
    window.addEventListener('scroll', handleScroll)
    resizeWindow()
    window.addEventListener('resize', resizeWindow)
  }
})

onUnmounted(() => {
  if (props.isSticky) {
    window.removeEventListener('scroll', handleScroll)
    window.removeEventListener('resize', resizeWindow)
  }
})
</script>

<template>
  <div
    id="top-bar-container"
    class="top-bar"
  >
    <div id="top-bar" :class="computedTopBarClass">
      <div class="container container--flex">
        <div></div>

        <div class="top-bar__right">
          <TopBarNavigation />
          <LanguageSwitcher type="negative" />
        </div>
      </div>
    </div>
    <div v-if="useStickyPlaceholder" id="stickyTopBarPlaceholder" />
  </div>
</template>
