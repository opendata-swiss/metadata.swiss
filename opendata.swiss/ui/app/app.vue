<template>
  <main id="main-content">
    <NuxtLayout>
      <Transition name="shrink-header">
        <OdsTopHeader
          v-if="!isMobileMenuOpen"
          :enable-authentication="true"
          :authenticated="authenticated"
          :username="username"
          @login="handleAuthEvent('login')"
          @logout="handleAuthEvent('logout')"
        />
      </Transition>
      <OdsHeader :navigation-items="navigationItems" @mobile-menu-state-change="mobileMenuOpened" />
      <Transition name="fade-content">
        <div v-if="!isMobileMenuOpen" style="min-height: calc(100dvh - 128px);">
          <NuxtPage :page-key="route => route.path" />
        </div>
      </Transition>
      <OdsFooter />
      <OdsBottomFooter />
    </NuxtLayout>
  </main>
</template>

<script setup lang="ts">

import OdsTopHeader from './components/headers/OdsTopHeader.vue'
import OdsHeader from './components/headers/OdsHeader.vue';
import OdsBottomFooter from '@/components/footer/OdsBottomFooter.vue'
import OdsFooter from './components/footer/OdsFooter.vue';
import type { OdsNavTabItem } from './components/headers/model/ods-nav-tab-item';
import { APP_NAVIGATION_ITEMS } from './constants/navigation-items';
import { useI18n } from '#imports';
import { useLocale as piveauLocale  } from '@piveau/sdk-vue' ;

import { onMounted, ref } from 'vue';
import { useNuxtApp } from '#app';

const nuxtApp = useNuxtApp();
const keycloakLogin = typeof nuxtApp.$keycloakLogin === 'function' ? nuxtApp.$keycloakLogin : undefined;
const keycloakLogout = typeof nuxtApp.$keycloakLogout === 'function' ? nuxtApp.$keycloakLogout : undefined;

function handleAuthEvent(event: 'login' | 'logout') {
  if (event === 'login' && keycloakLogin) {
    keycloakLogin();
  } else if (event === 'logout' && keycloakLogout) {
    keycloakLogout();
  }
}


const navigationItems = ref<OdsNavTabItem[]>(APP_NAVIGATION_ITEMS);
const isMobileMenuOpen = ref(false);

// Keycloak instance and authentication state
let keycloak = undefined;
if (import.meta.client) {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  keycloak = nuxtApp.$keycloak || ((window as any).keycloak);
}
const authenticated = ref(false);
const username = ref<string | undefined>(undefined);

if (import.meta.client && keycloak) {
  keycloak.onAuthSuccess = () => {
    authenticated.value = true;
    username.value = keycloak.tokenParsed?.preferred_username || keycloak.tokenParsed?.email || undefined;
  };
  keycloak.onAuthLogout = () => {
    authenticated.value = false;
    username.value = undefined;
  };
  // Set initial state if already authenticated
  if (keycloak.authenticated) {
    authenticated.value = true;
    username.value = keycloak.tokenParsed?.preferred_username || keycloak.tokenParsed?.email || undefined;
  }
}

const { locale } = useI18n();

const { setLocale, currentLocale} = piveauLocale();

watch(locale, (newLocale) => {
  if (newLocale !== (currentLocale as Ref<string>).value) {
    setLocale(newLocale) // set the piveau locale
  }
}, { immediate: true }
)

useHead({
  htmlAttrs: {
    lang: locale.value
  },
  bodyAttrs: {
    class: 'body--ods-brand'
  },
  link: [
    { rel: 'icon', type: 'image/png', sizes: '96x96', href: '/favicon-96x96.png' },
    { rel: 'icon', type: 'image/svg+xml', href: '/favicon.svg' },
    { rel: 'shortcut icon', href: '/favicon.ico' },
    { rel: 'apple-touch-icon', sizes: '180x180', href: '/apple-touch-icon.png' },
    { rel: 'manifest', href: '/site.webmanifest' },
  ],
  meta: [
    { name: 'apple-mobile-web-app-title', content: 'opendata.swiss' },
    // you can add other default meta tags, e.g. description, viewport, etc
  ]
})

function mobileMenuOpened(value: boolean) {
  isMobileMenuOpen.value = value;
}

function handleResize() {
  // Close the mobile menu if the window is resized to a width greater than or equal to 1024px
  if (window.innerWidth >= 1024) {
    isMobileMenuOpen.value = false
  }
}

onMounted(() => {
  window.addEventListener('resize', handleResize)
  handleResize()
})
</script>


<style lang="scss" scoped>


.shrink-header-enter-active, .shrink-header-leave-active {
  transition: max-height 1.0s cubic-bezier(0.4,0,0.2,1);
  overflow: hidden;
}
.shrink-header-enter-from, .shrink-header-leave-to {
  max-height: 0 !important;
}
.shrink-header-enter-to, .shrink-header-leave-from {
  max-height: 300px;
}


.fade-content-enter-active, .fade-content-leave-active {
  transition: opacity 0.6s cubic-bezier(0.4,0,0.2,1);
}
.fade-content-enter-from, .fade-content-leave-to {
  opacity: 0;
}
.fade-content-enter-to, .fade-content-leave-from {
  opacity: 1;
}

</style>
