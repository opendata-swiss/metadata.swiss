import {nodePolyfills} from 'vite-plugin-node-polyfills'
import {defineConfig} from "vite";
import {viteStaticCopy} from "vite-plugin-static-copy";

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
            if (filename.endsWith('config.yml') && process.env.NODE_ENV === 'development') {
              // In development, we want to use the local backend
              return `
local_backend:
  url: http://localhost:8088/api/v1

${content}`
            }

            return content
          }
        }
      ]
    }),
  ],
})
