import { z } from 'zod'
import { defineHubSearch, getTranslationFor } from '@piveau/sdk-vue'

const schemaOrganization = z.object({
  id: z.string().min(1),
  pref_label: z.record(z.string(), z.string()).optional().nullable(),
  name: z.record(z.string(), z.string()).optional().nullable(),
  description: z.record(z.string(), z.string()).optional().nullable(),
  identifier: z.string().optional().nullable(),
})

type Organization = z.infer<typeof schemaOrganization>

function firstAvailableLocale(record: Record<string, string> | null | undefined): string | undefined {
  if (!record) return undefined
  const keys = Object.keys(record)
  return keys.length > 0 ? keys[0] : undefined
}

function resolveLocalizedField(
  field: Record<string, string> | null | undefined,
  currentLocale: string,
): string | undefined {
  const locales = [currentLocale, 'en', firstAvailableLocale(field)].filter((locale): locale is string => Boolean(locale))
  return getTranslationFor(field, locales)
}

function resolveDisplayLabel(organization: Organization, currentLocale: string): string {
  return resolveLocalizedField(organization.pref_label, currentLocale)
    || resolveLocalizedField(organization.name, currentLocale)
    || organization.identifier
    || organization.id
}

function resolveDescription(organization: Organization, currentLocale: string): string | undefined {
  return resolveLocalizedField(organization.description, currentLocale)
}

/**
 * Returns a Piveau Hub Search definition for organization discovery.
 */
export function useOrganizationsSearch() {
  const baseUrl = useRuntimeConfig().public.piveauHubSearchUrl as string

  return defineHubSearch({
    baseUrl,
    index: 'organization',
    indexDetails: 'organizations',
    schema: schemaOrganization,
  }, (organization, localeInstance) => {
    const currentLocale = localeInstance.currentLocale

    return {
      getId: organization.id,
      getTitle: resolveDisplayLabel(organization, currentLocale),
      getDescription: resolveDescription(organization, currentLocale),
    }
  })
}
