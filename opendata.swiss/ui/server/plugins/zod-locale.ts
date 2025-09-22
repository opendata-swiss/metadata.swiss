import { defineNitroPlugin } from '#imports'
import { config } from 'zod'
import { en, de, fr, it } from 'zod/locales'
import acceptLanguage from 'accept-language';

acceptLanguage.languages(['de', 'fr', 'it', 'en'])

const languages = new Map(Object.entries({ en, de, fr, it }))

export default defineNitroPlugin((nitro) => {
  nitro.hooks.hook('request', ({ headers }) => {
    const language = acceptLanguage.get(headers.get('accept-language'))
    const locale = languages.get(language || 'de') || de
    config(locale())
  })
})
