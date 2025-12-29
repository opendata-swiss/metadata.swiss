import * as path from 'node:path'
import { resolve } from 'node:path'

const __dirname = path.dirname(new URL(import.meta.url).pathname)

// https://nuxt.com/docs/api/configuration/nuxt-config
export default defineNuxtConfig({
  modules: [
    '@nuxt/eslint',
    '@nuxt/content',
    '@pinia/nuxt',
    '@nuxtjs/i18n',
    '@nuxt/image',
    '@nuxt/icon',
  ],
  plugins: [],
  pages: {
    enabled: true,
  },
  components: {
    global: true,
    dirs: [
      '~/components/content',
    ],
  },
  devtools: { enabled: true },
  css: [
    '~/assets/main.css',
  ],
  content: {
    build: {
      markdown: {
        toc: {
          depth: 3,
          searchDepth: 3,
        },
      },
    },
  },
  mdc: {
    components: {
      map: {
        a: 'OdsProseA',
        h2: 'OdsProseH2',
        h3: 'OdsProseH3',
        h4: 'OdsProseH4',
      },
    },
  },
  runtimeConfig: {
    public: {
      rootDir: __dirname,
      piveauHubRepoUrl: 'https://piveau-hub-repo.int.ods.zazukoians.org/',
      piveauHubSearchUrl: 'https://piveau-hub-search.int.ods.zazukoians.org/',
    },
  },
  dir: {
    pages: resolve(import.meta.dirname, 'pages'),
  },
  build: {
    transpile: ['form-data'],
  },
  routeRules: {
    '*/showcases/submit': { ssr: false },
  },
  compatibilityDate: '2025-07-15',
  nitro: {
    devProxy: {
      '/admin/': 'http://localhost:5173/admin/',
    },
    plugins: [
      '~~/server/plugins/zod-locale',
    ],
  },
  eslint: {
    config: {
      stylistic: true,
    },
  },
  i18n: {
    defaultLocale: 'de',
    strategy: 'prefix',
    locales: [
      { code: 'de', name: 'Deutsch', file: 'de.json' },
      { code: 'en', name: 'English', file: 'en.json' },
      { code: 'fr', name: 'Francais', file: 'fr.json' },
      { code: 'it', name: 'Itlaliano', file: 'it.json' },
    ],
    experimental: {
      localeDetector: 'localeDetector.ts',
    },
  },
  icon: {
    mode: 'svg',
    customCollections: [
      {
        prefix: 'ods',
        dir: './app/assets/icons',
        normalizeIconName: false,
      },
    ],
  },
})
