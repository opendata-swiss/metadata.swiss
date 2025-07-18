<template>

    <AlterBodyClasses :isMobileMenuOpen="layoutStore.mobileMenuIsOpen" :isFreebrand="true"/>
    <header id="main-header">
      <a href="#main-content" class="skip-to-content">Skip to main content</a>
      <TopBar />
      <TopHeader :isFreebrand="true" />
      <DesktopMenu :menuItems="menuItems" :isSimplePage="true" :showActiveNavigation="true" />
      <MobileMenu :showActiveNavigation="true" />
    </header>
    <main id="main-content">
        <RouterView />
    </main>
    <footer id="main-footer" class="footer">
      <FooterInformation />
      <FooterNavigation />
    </footer>

</template>

<script setup lang="ts">
import { ref } from 'vue'
import AlterBodyClasses from './components/ch/objects/AlterBodyClasses.vue'
import TopBar from './components/ch/sections/TopBar.vue'
import TopHeader from './components/ch/sections/TopHeader.vue'
import DesktopMenu from './components/ch/sections/DesktopMenu.vue'
import MobileMenu from './components/ch/sections/MobileMenu.vue'
import FooterInformation from './components/ch/sections/FooterInformation.vue'
import FooterNavigation from './components/ch/sections/FooterNavigation.vue'
import type { MenuItem } from './model/menu-item'
import { useLayoutStore } from './store/layout'

const layoutStore = useLayoutStore()

const menuItems = ref<MenuItem[]>([
  { label: 'Home', to: '/' },
  { label: 'Datasets', to: '/datasets' },
]
)



</script>
