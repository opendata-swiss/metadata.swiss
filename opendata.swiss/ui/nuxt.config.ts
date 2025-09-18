import vuetify, { transformAssetUrls } from 'vite-plugin-vuetify'

import { resolve } from 'node:path'

const { PIVEAU_HUB_REPO_URL, PIVEAU_HUB_SEARCH_URL} = process.env

// https://nuxt.com/docs/api/configuration/nuxt-config
export default defineNuxtConfig({
  runtimeConfig: {
    public: {
      PIVEAU_HUB_REPO_URL,
      PIVEAU_HUB_SEARCH_URL
    }
  },
  compatibilityDate: '2025-07-15',
  devtools: { enabled: true },
  modules: [
    '@nuxt/eslint',
    '@nuxt/content',
    '@pinia/nuxt',
    '@nuxtjs/i18n',
    '@nuxt/image',
    (_options, nuxt) => {
      nuxt.hooks.hook('vite:extendConfig', (config) => {
        config.plugins?.push(vuetify({ autoImport: true }))
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
        },
      }
    }
  },
  mdc: {
    components: {
      map: {
        a: 'OdsProseA',
        h2: 'OdsProseH2',
        h3: 'OdsProseH3',
        h4: 'OdsProseH4',
      }
    }
  },
  components: {
    global: true,
    dirs: [
      '~/components/content',
    ]
  },
  build: {
    transpile: ['vuetify', 'form-data'],
  },
  vite: {
    vue: {
      template: {
        transformAssetUrls,
      },
    },
  },
  plugins: [],
  i18n: {
    defaultLocale: 'de',
    strategy: 'prefix',
    locales: [
      { code: 'de', name: 'Deutsch', file: 'de.json' },
      { code: 'en', name: 'English', file: 'en.json' },
      { code: 'fr', name: 'Francais', file: 'fr.json' },
      { code: 'it', name: 'Itlaliano', file: 'it.json' },
    ]
  },
  nitro: {
    devProxy: {
      '/admin/': 'http://localhost:5173/admin/',
    }
  }
})
