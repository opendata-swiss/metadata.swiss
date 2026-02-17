import { nodePolyfills } from 'vite-plugin-node-polyfills'
import { defineConfig } from 'vite'
import { viteStaticCopy } from 'vite-plugin-static-copy'
import yaml from 'js-yaml'

const piveauHubSearch = process.env.NUXT_PUBLIC_PIVEAU_HUB_SEARCH_URL

interface Field {
  label: string
  name: string
  widget: string
}
interface PiveauVocabularyField extends Field {
  widget: 'piveau-vocabulary'
  piveau: {
    search: string
  }
}

interface DecapConfig {
  backend: {
    repo: string
  }
  local_backend?: {
    url: string
  }
  media_folder: string
  collections: Array<{
    folder: string
    fields: Array<Field | PiveauVocabularyField>
  }>
}

export default defineConfig({
  base: '/admin',
  build: {
    outDir: '../../public/admin',
  },
  plugins: [
    nodePolyfills(),
    viteStaticCopy({
      targets: [
        {
          src: 'config.yml',
          dest: '',
          transform(content, filename) {
            if (!filename.endsWith('config.yml')) {
              return content
            }

            const config = yaml.load(content) as DecapConfig

            if (process.env.NODE_ENV === 'development') {
              // In development, we want to use the local backend
              // and local piveau, if configured in .env

              config.local_backend = {
                url: 'http://localhost:8088/api/v1',
              }

              // add path leading to the local content clone
              config.media_folder = `opendata.swiss/ui/content/${config.media_folder}`

              for (const collection of config.collections) {
                collection.folder = `opendata.swiss/ui/content/${collection.folder}`

                for (const field of collection.fields) {
                  if ('piveau' in field && piveauHubSearch) {
                    field.piveau.search = piveauHubSearch
                  }
                }
              }
            }
            else {
              config.backend.repo = `${process.env.GITHUB_OWNER}/${process.env.GITHUB_REPO}`
            }

            return yaml.dump(config)
          },
        },
      ],
    }),
  ],
})
