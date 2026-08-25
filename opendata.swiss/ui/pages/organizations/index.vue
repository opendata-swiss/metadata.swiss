<script setup lang="ts">
import { computed, ref } from 'vue'

import OdsPage from '../../app/components/OdsPage.vue'
import OdsBreadcrumbs from '../../app/components/OdsBreadcrumbs.vue'
import OdsSearchPanel from '../../app/components/OdsSearchPanel.vue'
import OdsOrganizationTree from '../../app/components/organizations/OdsOrganizationTree.vue'
import { homePageBreadcrumb } from '../../app/composables/breadcrumbs'
import { useFetch, useRuntimeConfig, useSeoMeta } from 'nuxt/app'
import { useI18n } from 'vue-i18n'

interface OrganizationItem {
  id: string
  resource: string
  sub_organization_of?: string[]
  pref_label?: Record<string, string>
  name?: Record<string, string>
  ancestors?: Array<{
    id?: string
    resource?: string
    name?: Record<string, string>
    pref_label?: Record<string, string>
  }>
}

interface OrganizationTreeNode {
  id: string
  organization: OrganizationItem
  children: OrganizationTreeNode[]
}

interface HubSearchOrganizationResponse {
  result?: {
    count?: number
    results?: OrganizationItem[]
  }
}

interface HubSearchFacetItem {
  id: string
  count: number
}

interface HubSearchFacetGroup {
  id: string
  items: HubSearchFacetItem[]
}

interface HubSearchDatasetFacetsResponse {
  result?: {
    facets?: HubSearchFacetGroup[]
  }
}

const { t, locale } = useI18n()

const searchInput = ref('')
const baseUrl = useRuntimeConfig().public.piveauHubSearchUrl as string

const { data, pending, error } = await useFetch<HubSearchOrganizationResponse>(() => `${baseUrl}search`, {
  query: {
    filter: 'organization',
    limit: 1000,
  },
})

const { data: datasetFacets } = await useFetch<HubSearchDatasetFacetsResponse>(() => `${baseUrl}search`, {
  query: {
    filter: 'dataset',
    limit: 0,
  },
})

function getLocalizedValue(value?: Record<string, string>) {
  if (!value) {
    return ''
  }

  return value[locale.value] || Object.values(value)[0] || ''
}

function getOrganizationLabel(organization: OrganizationItem) {
  return getLocalizedValue(organization.name) || getLocalizedValue(organization.pref_label) || organization.id
}

function getIdFromReference(reference: string) {
  return reference.split('/').filter(Boolean).at(-1) || reference
}

function getParentId(organization: OrganizationItem) {
  const directParent = organization.sub_organization_of?.[0]

  if (directParent) {
    return getIdFromReference(directParent)
  }

  const parentFromAncestors = organization.ancestors?.at(-1)

  if (!parentFromAncestors) {
    return undefined
  }

  if (parentFromAncestors.id) {
    return parentFromAncestors.id
  }

  if (parentFromAncestors.resource) {
    return getIdFromReference(parentFromAncestors.resource)
  }

  return undefined
}

const organizations = computed(() => data.value?.result?.results ?? [])

const sortedOrganizations = computed(() => {
  const collator = new Intl.Collator(locale.value)

  return [...organizations.value].sort((a, b) => {
    const labelA = getOrganizationLabel(a)
    const labelB = getOrganizationLabel(b)
    return collator.compare(labelA, labelB)
  })
})

function sortTree(nodes: OrganizationTreeNode[]) {
  const collator = new Intl.Collator(locale.value)

  nodes.sort((a, b) => {
    return collator.compare(getOrganizationLabel(a.organization), getOrganizationLabel(b.organization))
  })

  for (const node of nodes) {
    sortTree(node.children)
  }
}

const organizationTree = computed<OrganizationTreeNode[]>(() => {
  const nodesById = new Map<string, OrganizationTreeNode>()

  for (const organization of sortedOrganizations.value) {
    nodesById.set(organization.id, {
      id: organization.id,
      organization,
      children: [],
    })
  }

  const roots: OrganizationTreeNode[] = []

  for (const node of nodesById.values()) {
    const parentId = getParentId(node.organization)
    const parentNode = parentId ? nodesById.get(parentId) : undefined

    if (parentNode && parentNode.id !== node.id) {
      parentNode.children.push(node)
      continue
    }

    roots.push(node)
  }

  sortTree(roots)
  return roots
})

const datasetCountByOrganizationId = computed<Record<string, number>>(() => {
  const counts: Record<string, number> = {}
  const organizationFacet = datasetFacets.value?.result?.facets?.find(facet => facet.id === 'organization')

  if (!organizationFacet) {
    return counts
  }

  for (const item of organizationFacet.items) {
    counts[item.id] = item.count
  }

  return counts
})

const filteredOrganizations = computed(() => {
  const searchTerm = searchInput.value.trim().toLowerCase()

  if (!searchTerm) {
    return sortedOrganizations.value
  }

  return sortedOrganizations.value.filter((organization) => {
    const localizedName = getLocalizedValue(organization.name).toLowerCase()
    const localizedLabel = getLocalizedValue(organization.pref_label).toLowerCase()

    return localizedName.includes(searchTerm)
      || localizedLabel.includes(searchTerm)
      || organization.id.toLowerCase().includes(searchTerm)
  })
})

const matchingOrganizationIds = computed(() => {
  return new Set(filteredOrganizations.value.map(organization => organization.id))
})

function filterTree(nodes: OrganizationTreeNode[], matches: Set<string>): OrganizationTreeNode[] {
  return nodes
    .map((node) => {
      const filteredChildren = filterTree(node.children, matches)

      if (matches.has(node.id) || filteredChildren.length > 0) {
        return {
          ...node,
          children: filteredChildren,
        }
      }

      return null
    })
    .filter((node): node is OrganizationTreeNode => node !== null)
}

const filteredOrganizationTree = computed(() => {
  return filterTree(organizationTree.value, matchingOrganizationIds.value)
})

const breadcrumbs = [
  await homePageBreadcrumb(locale),
  {
    title: t('message.header.navigation.organizations'),
    path: '/organizations',
  },
]

useSeoMeta({
  title: `${t('message.header.navigation.organizations')} | opendata.swiss`,
})
</script>

<template>
  <OdsPage :hero="{ title: t('message.header.navigation.organizations') }">
    <template #header>
      <OdsBreadcrumbs :breadcrumbs="breadcrumbs" />
    </template>

    <OdsSearchPanel
      :search-input="searchInput"
      :search-prompt="t('message.organizations.search_placeholder')"
      :title="t('message.header.navigation.organizations')"
      @search="(value) => searchInput = value"
      @update:search-input="(value) => searchInput = Array.isArray(value) ? (value[0] || '') : value"
    />

    <section class="section section--default">
      <div class="container">
        <p class="organization-count">
          <strong>{{ filteredOrganizations.length }}</strong>
          {{ t('message.header.navigation.organizations') }}
        </p>

        <p
          v-if="error"
          class="notification notification--danger"
        >
          {{ t('message.organizations.load_error') }}
        </p>

        <OdsOrganizationTree
          v-else-if="!pending && filteredOrganizations.length > 0"
          :nodes="filteredOrganizationTree"
          :dataset-count-by-organization-id="datasetCountByOrganizationId"
        />

        <p
          v-else-if="!pending"
          class="notification notification--info"
        >
          {{ t('message.organizations.empty') }}
        </p>
      </div>
    </section>
  </OdsPage>
</template>

<style lang="scss" scoped>
.organization-count {
  margin-bottom: 1.5rem;
}
</style>
