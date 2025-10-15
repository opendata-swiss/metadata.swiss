/**
 * Available application languages
 */
export const APP_LANGUAGES = ['de', 'fr', 'it', 'en'] as const

export type AppLanguage = typeof APP_LANGUAGES[number];
