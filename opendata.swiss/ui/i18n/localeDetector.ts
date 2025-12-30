import type { AppLanguage } from '~/constants/langages'
import { APP_LANGUAGES } from '~/constants/langages'

export default defineI18nLocaleDetector((event, config) => {
  // try to get locale from header (`accept-header`)
  const header = tryHeaderLocale(event, { lang: '' }) // disable locale default value with `lang` option
  if (APP_LANGUAGES.includes(header?.toString() as AppLanguage)) {
    return header!.toString()
  }

  // If the locale cannot be resolved up to this point, it is resolved with the value `defaultLocale` of the locale config passed to the function
  return config.defaultLocale
})
