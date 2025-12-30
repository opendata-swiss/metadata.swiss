// @ts-check
import reactPlugin from 'eslint-plugin-react'
import withNuxt from './.nuxt/eslint.config.mjs'

export default withNuxt(
  {
    settings: {
      react: {
        version: 'detect',
      },
    },
    rules: {
      '@stylistic/no-tabs': 'off',
    },
  },
  reactPlugin.configs.flat.recommended,
)
