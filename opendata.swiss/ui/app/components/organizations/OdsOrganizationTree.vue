<script setup lang="ts">
interface OrganizationItem {
  id: string
  resource: string
  pref_label?: Record<string, string>
  name?: Record<string, string>
}

interface OrganizationTreeNode {
  id: string
  organization: OrganizationItem
  children: OrganizationTreeNode[]
}

const props = defineProps<{
  nodes: OrganizationTreeNode[]
  datasetCountByOrganizationId?: Record<string, number>
}>()

const { t, locale } = useI18n()
const localePath = useLocalePath()

function getLocalizedValue(value?: Record<string, string>) {
  if (!value) {
    return ''
  }

  return value[locale.value] || Object.values(value)[0] || ''
}

function organizationLabel(organization: OrganizationItem) {
  return getLocalizedValue(organization.name) || getLocalizedValue(organization.pref_label) || organization.id
}

function datasetsLink(organization: OrganizationItem) {
  return {
    path: localePath('/datasets'),
    query: {
      publisher: organization.name ? organization.name.de : '',
    },
  }
}

function getDatasetCount(organizationId: string) {
  return props.datasetCountByOrganizationId?.[organizationId] || 0
}
</script>

<template>
  <ul class="organization-tree">
    <li
      v-for="node in props.nodes"
      :key="node.id"
      class="organization-tree__node"
    >
      <div class="organization-tree__row">
        <div>
          <h2 class="h5 organization-tree__title">
            {{ organizationLabel(node.organization) }}
          </h2>
          <p class="organization-tree__meta">
            {{ node.organization.id }}
          </p>
          <p class="organization-tree__meta">
            {{ t('message.organizations.datasets_count', { count: getDatasetCount(node.organization.id) }) }}
          </p>
        </div>

        <NuxtLink
          class="organization-tree__link"
          :to="datasetsLink(node.organization)"
        >
          {{ t('message.organizations.show_datasets') }}
        </NuxtLink>
      </div>

      <OdsOrganizationTree
        v-if="node.children.length > 0"
        :nodes="node.children"
        :dataset-count-by-organization-id="props.datasetCountByOrganizationId"
      />
    </li>
  </ul>
</template>

<style lang="scss" scoped>
.organization-tree {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: 0.75rem;
}

.organization-tree .organization-tree {
  margin-top: 0.75rem;
  margin-left: 1rem;
  padding-left: 1rem;
  border-left: 2px solid var(--color-border, #d6d9dd);
}

.organization-tree__node {
  border: 1px solid var(--color-border, #d6d9dd);
  border-radius: 0.75rem;
  padding: 0.85rem;
  background-color: #fff;
}

.organization-tree__row {
  display: grid;
  gap: 0.5rem;
}

.organization-tree__title {
  margin: 0;
}

.organization-tree__meta {
  margin: 0.25rem 0 0;
  color: var(--color-text-muted, #5a6270);
}

.organization-tree__link {
  font-weight: 600;
  justify-self: start;
}

@media (min-width: 768px) {
  .organization-tree__row {
    grid-template-columns: 1fr auto;
    align-items: center;
  }
}
</style>
