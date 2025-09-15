<script setup lang="ts">
import { ref, watch } from 'vue';
import { useI18n } from 'vue-i18n';

import type { OdsNavTabItem } from '@/components/headers/model/ods-nav-tab-item';
import NamedLogo from '~/components/NamedLogo.vue';
import LogoSmall from '@/components/LogoSmall.vue';
import BurgerButton from '@/components/BurgerButton.vue';
import OdsNavigationPanel from '@/components/OdsNavigationPanel.vue';
import { NuxtLinkLocale } from '#components';
const localePath = useLocalePath()

const { locale } = useI18n()
const route = useRoute();

interface Props {
  enableAuthentication?: boolean;
  authenticated?: boolean;
  navigationItems: OdsNavTabItem[];
}

const props = withDefaults(defineProps<Props>(), {
  enableAuthentication: false,
  authenticated: false,
  navItems: []
});


const emit = defineEmits<{
  (e: 'mobileMenuStateChange', value: boolean): void;
}>();

const { t } = useI18n();

const selectedTab = ref<number | null>(null);
const menuOpen = ref(false);


function setCurrentItemToMenuItem (tabIndex: number) {
   selectedTab.value = tabIndex;
}

watch(menuOpen, (val) => {
  emit('mobileMenuStateChange', val);
});


function closeMobileMenu() {
  menuOpen.value = false;
  emit('mobileMenuStateChange', false);
}

function isChildPage({ to }: OdsNavTabItem) {
  if(to === '/') {
    return route.path === `/${locale.value}`
  }

  return to && route.path.startsWith(`/${locale.value}${to}`)
}
</script>

<template>
  <NuxtLoadingIndicator color="red" />
  <div id="top-header-id" class="top-header">
    <div class="container container--flex">
      <div class="logo" to="/" aria-label="Home">
        <NuxtLinkLocale class="logo" to="/" aria-label="Home"><LogoSmall style="margin-right:4px;"/></NuxtLinkLocale>
        <NuxtLinkLocale class="logo" to="/" aria-label="Home"><NamedLogo/></NuxtLinkLocale>
      <div class="logo__separator" role="separator" aria-hidden="true"/>
      <div class="logo-title__container">
        <div class="logo__accronym">ODS Portal</div>
        <div class="logo__title">
          <div>opendata.swiss<br>Portal</div>
        </div>
      </div>
      </div>
      <div class="top-header__right">
        <BurgerButton v-model="menuOpen"  title="Toggle mobile menu" />
      </div>
     </div>
  </div>
  <div id="desktop-navigation-id">
    <div class="container container--flex">
      <nav id="main-navigation" class="main-navigation main-navigation--desktop">
      <ul>
        <template v-for="(item, index) in props.navigationItems" :key="item.label">
        <!-- a normal tab -->
        <li v-if="!item.subMenu"  class="tab" @click="setCurrentItemToMenuItem(index)">
          <NuxtLinkLocale :class="{ active: isChildPage(item) }" :to="item.to"><span> {{ t(item.label) }}</span></NuxtLinkLocale>
        </li>
        <li  v-if="item.subMenu"  class="tab">
          <v-menu>
              <template #activator="{ props: menuProps }">
                <a v-bind="menuProps" :class="{ active: selectedTab === index }"><span>{{ t(item.label) }}</span></a>
              </template>
              <v-list>
                <v-list-item
                  v-for="subItem in item.subMenu"
                  :key="subItem.label"
                  :to="localePath(subItem.to as string)"
                  @click="setCurrentItemToMenuItem(index)"
                >
                  {{ t(subItem.label) }}
                </v-list-item>
              </v-list>
            </v-menu>
          </li>

        </template>
      </ul>
      </nav>
    </div>
  </div>


  <!-- Mobile menu -->
  <Transition name="fade-menu">
    <div v-if="menuOpen" class="ods-mobile-menu"  >
      <OdsNavigationPanel  :items="props.navigationItems" @request-close="closeMobileMenu" />
    </div>
  </Transition>
</template>

<style lang="scss" scoped>
@use '@/assets/ods/ods_breakpoints.scss' as mdeia;

// keep
#desktop-navigation-id{
  border-bottom: 1px solid rgb(229, 231, 235);
}
a {
  text-decoration: none;
}

.tab {
  display: block;
  margin-top: 0 !important;
  a {
    display: flex;
    align-items: center;
    justify-content: center;
  }
}

// end keep
.ods-mobile-menu {
   @include mdeia.respond-to-xs {
    display: flex;

    height: calc(100vh - 65px);
    background-color: var(--ods-light-dark-background-color);
  }

  @media (min-width: 1024px) {
    display: none;
  }
}

.fade-menu-enter-active {
  transition: opacity 0.4s cubic-bezier(0.4,0,0.2,1);
  transition-delay: 0.5s;
}
.fade-menu-leave-active {
  transition: opacity 0.4s cubic-bezier(0.4,0,0.2,1);
  transition-delay: 0s;
}
.fade-menu-enter-from, .fade-menu-leave-to {
  opacity: 0;
}
.fade-menu-enter-to, .fade-menu-leave-from {
  opacity: 1;
}


</style>
