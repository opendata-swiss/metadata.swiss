import type { H3Event } from 'h3'

import acceptLanguage from 'accept-language'

acceptLanguage.languages(['de', 'fr', 'it', 'en'])

export function getLanguage({ headers }: H3Event): string {
  return acceptLanguage.get(headers.get('accept-language')) || 'de'
}
