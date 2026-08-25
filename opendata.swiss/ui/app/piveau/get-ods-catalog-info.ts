import type { Dataset } from '@piveau/sdk-core'
import type { LocaleInstance } from '@piveau/sdk-vue'
import { toValue } from 'vue'

export interface Catalog {
  id: string
  modified: string
  issued: string
  title: string
  publisher: CatalogPublisher
  description: string
  facetInfo?: CatalogFacetGroup[]
  record: CatalogRecord
}

interface CatalogPublisher {
  id: string
  name: string
}

export interface CatalogFacetGroup {
  id: string
  title?: string | Record<string, string>
  items: CatalogFacetItem[]
}

interface CatalogFacetItem {
  id: string
  title?: string | Record<string, string>
  count: number
}

interface CatalogRecord {
  modified: string
  issued: string
}

function getFacetItemTitle(item: CatalogFacetItem, locale: string): string {
  if (!item.title) {
    return ''
  }

  if (typeof item.title === 'string') {
    return item.title
  }

  return item.title[locale] ?? Object.values(item.title)[0] ?? ''
}

function normalizeValue(value: string): string {
  return value.trim().toLowerCase()
}

function resolvePublisherId(publisherName: string, locale: string, facetInfo?: CatalogFacetGroup[]): string {
  if (!publisherName || !facetInfo || facetInfo.length === 0) {
    return ''
  }

  const organizationFacet = facetInfo.find(facet => facet.id === 'organization' || facet.id === 'publisher')
  if (!organizationFacet) {
    return ''
  }

  const normalizedPublisherName = normalizeValue(publisherName)
  const matchingItem = organizationFacet.items.find((item) => {
    const title = getFacetItemTitle(item, locale)
    return normalizeValue(title) === normalizedPublisherName
  })

  return matchingItem?.id ?? ''
}

/**
 * Extend enriched dataset with catalog information. This includes title and description in the current locale and the catalog record issued and modified dates.
 *
 * @param dataset dataset
 * @param localeInstance
 * @param facetInfo optional facet groups from query response (including item counts)
 * @returns Catalog information (id, modified, issued, title, description, facetInfo, record: {modified, issued})
 */
export function getOdsCatalogInfo(dataset: Dataset, localeInstance: LocaleInstance, facetInfo?: CatalogFacetGroup[]): Catalog {
  const catalogInfo = dataset.catalog
  const recordInfo = dataset.catalog_record

  const locale = toValue(localeInstance.currentLocale)
  const availableLocalesForTitle = Object.keys(catalogInfo?.title || {})
  const availableLocalesForDescription = Object.keys(catalogInfo?.description || {})
  const availableLocalesForPublisher = Object.keys(catalogInfo?.publisher?.name || {})

  let title = ''
  let description = ''
  let publisher = ''

  if (availableLocalesForTitle.includes(locale)) {
    title = catalogInfo?.title?.[locale] ?? ''
  }
  else if (availableLocalesForTitle.length > 0 && availableLocalesForTitle[0] !== undefined) {
    title = catalogInfo?.title?.[availableLocalesForTitle[0] as string] ?? ''
  }

  if (availableLocalesForDescription.includes(locale)) {
    description = catalogInfo?.description?.[locale] ?? ''
  }
  else if (availableLocalesForDescription.length > 0 && availableLocalesForDescription[0] !== undefined) {
    description = catalogInfo?.description?.[availableLocalesForDescription[0] as string] ?? ''
  }

  if (availableLocalesForPublisher.includes(locale)) {
    const publisherNameByLocale = catalogInfo?.publisher?.name as Record<string, string> | undefined
    publisher = publisherNameByLocale?.[String(locale)] ?? ''
  }
  else if (availableLocalesForPublisher.length > 0 && availableLocalesForPublisher[0] !== undefined) {
    const publisherNameByLocale = catalogInfo?.publisher?.name as Record<string, string> | undefined
    publisher = publisherNameByLocale?.[availableLocalesForPublisher[0]] ?? ''
  }

  const publisherId = resolvePublisherId(publisher, locale, facetInfo)

  return {
    id: catalogInfo.id,
    modified: catalogInfo.modified ?? '',
    issued: catalogInfo.issued ?? '',
    title,
    publisher: {
      id: publisherId,
      name: publisher,
    },
    description,
    facetInfo,
    record: {
      modified: recordInfo?.modified ?? '',
      issued: recordInfo?.issued ?? '',
    },
  }
}
