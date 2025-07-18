<template>
  <div id="top-header-id" class="top-header">
    <div class="container container--flex">
      <Logo
        title="opendata.swiss<br />Portal"
        accronym="opndata.swiss"
        :class="overrideLogoForPrint ? 'logo--print-hidden' : ''"
        :isFreebrand="isFreebrand"
      />
      <Logo
        v-if="overrideLogoForPrint"
        title="opendata.swiss<br />Portal"
        accronym="SECO"
        :class="overrideLogoForPrint ? 'logo--print-only' : ''"
      />

      <div class="top-header__right">
        <MetaNavigation :isFreebrand="isFreebrand" />
        <div class="top-header__container-flex">
          <SearchMain :isMenuV2="isMenuV2" @toggle-search="toggleSearch" />
        </div>
        <div
          class="top-header__shopping-cart-button-mobile"
          :class="isFreebrand ? 'freebrand' : ''"
        >

        </div>
        <BurgerButton
          :isOpen="getMobileMenuIsOpen()"
          @click="layoutStore.toggleMobileMenu"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import { useLayoutStore } from '../../../store/layout'
import BurgerButton from '../components/BurgerButton.vue'
import Logo from '../components/Logo.vue'
import SearchMain from '../components/SearchMain.vue'
import MetaNavigation from '../navigations/MetaNavigation.vue'

const layoutStore = useLayoutStore()

const screenSize = ref(0)

const props = defineProps({
  overrideLogoForPrint: {
    type: String,
    default: () => '',
  },
  isFreebrand: {
    type: Boolean,
    default: () => false,
  },
  isMenuV2: {
    type: Boolean,
    default: () => false,
  },
})
const emit = defineEmits(['top-header-search-toggle'])

function toggleSearch() {
  // Toggles v2 search input bellow top header
  emit('top-header-search-toggle')
}

function getMobileMenuIsOpen() {
  /* Disable menu animation for new mobile menu */
  if (props.isMenuV2) {
    return false
  }
  return layoutStore.mobileMenuIsOpen
}

function resizeWindow() {
  screenSize.value = document.body.clientWidth
}

onMounted(() => {
  resizeWindow()
  window.addEventListener('resize', resizeWindow)
})

onUnmounted(() => {
  window.removeEventListener('resize', resizeWindow)
})
</script>
