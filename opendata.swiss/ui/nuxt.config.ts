import vuetify, {transformAssetUrls} from 'vite-plugin-vuetify'

import {resolve} from 'node:path'

// https://nuxt.com/docs/api/configuration/nuxt-config
export default defineNuxtConfig({
  compatibilityDate: '2025-07-15',
  devtools: {enabled: true},
  modules: [
    '@nuxt/eslint',
    '@nuxt/content',
    '@pinia/nuxt',
    '@nuxtjs/i18n',
    '@nuxt/image',
    (_options, nuxt) => {
      nuxt.hooks.hook('vite:extendConfig', (config) => {
        config.plugins?.push(vuetify({autoImport: true}))
      })
    },
  ],
  css: [
    '~/assets/main.css',
  ],
  dir: {
    pages: resolve(import.meta.dirname, 'pages'),
  },
  pages: {
    enabled: true,
  },
  content: {
    build: {
      markdown: {
        toc: {
          depth: 3,
          searchDepth: 3,
        }
      }
    }
  },
  components: {
    global: true,
    dirs: [
      '~/components',
      '~/components/content',
    ]
  },
  routeRules: {
    '/datasets/**': {ssr: false},
    '/*/datasets/**': {ssr: false}
  },
  build: {
    transpile: ['vuetify'],
  },
  vite: {
    vue: {
      template: {
        transformAssetUrls,
      },
    },
  },
  plugins: [
    {src: '~/plugins/piveau', mode: 'client'},
  ],
  i18n: {
    defaultLocale: 'de',
    strategy: 'prefix',
    locales: [
      {code: 'de', name: 'Deutsch', file: 'de.json'},
      {code: 'en', name: 'English', file: 'en.json'},
      {code: 'fr', name: 'Francais', file: 'fr.json'},
      {code: 'it', name: 'Itlaliano', file: 'it.json'},
    ]
  },
  nitro: {
    devProxy: {
      '/admin/': 'http://localhost:5173/admin/',
    }
  }
})
