<script setup lang="ts">
import OdsButton from '../OdsButton.vue'
import SvgIcon from '../SvgIcon.vue'

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
const expandedNodeIds = ref<Record<string, boolean>>({})

function getLocalizedValue(value?: Record<string, string>) {
  if (!value) {
    return ''
  }

  return value[locale.value] || Object.values(value)[0] || ''
}

function organizationLabel(organization: OrganizationItem) {
  return getLocalizedValue(organization.name) || getLocalizedValue(organization.pref_label) || organization.id
}

function organizationLabelShort(organization: OrganizationItem) {
  const label = getLocalizedValue(organization.name) || getLocalizedValue(organization.pref_label) || organization.id
  const parts = label
    .split(/[\s-]+/)
    .filter(Boolean)

  const uppercaseInitials = parts
    .filter(part => /^[A-ZÀ-ÖØ-Þ]/.test(part))
    .map(part => part.charAt(0))

  if (uppercaseInitials.length >= 2) {
    return `${uppercaseInitials[0]}${uppercaseInitials[1]}`.toUpperCase()
  }

  if (parts.length >= 2) {
    return `${parts[0]!.charAt(0)}${parts[1]!.charAt(0)}`.toUpperCase()
  }

  return label.replace(/\s+/g, '').slice(0, 2).toUpperCase()
}

function datasetsLink(organization: OrganizationItem) {
  return {
    path: localePath('/datasets'),
    query: {
      organization: organization.id,
    },
  }
}

function getDatasetCount(organizationId: string) {
  return props.datasetCountByOrganizationId?.[organizationId] || 0
}

function isNodeExpanded(nodeId: string) {
  return expandedNodeIds.value[nodeId] ?? true
}

function toggleNode(nodeId: string) {
  expandedNodeIds.value[nodeId] = !isNodeExpanded(nodeId)
}
</script>

<template>
  <ul class="organization-tree">
    <li
      v-for="node in props.nodes"
      :key="node.id"
      class="organization-tree__node"
    >
      <div class="column">
        <div class="row">
          <div class="avatar">
            {{ organizationLabelShort(node.organization) }}
          </div>
          <div class="org-props">
            <h2 class="h6 org-title">
              {{ organizationLabel(node.organization) }}
            </h2>
            <div>
              <NuxtLink
                v-if="getDatasetCount(node.organization.id) > 0"
                class="organization-tree__link"
                :to="datasetsLink(node.organization)"
                :title=" t('message.organizations.show_datasets')"
              >
                {{ t('message.organizations.datasets_count', { count: getDatasetCount(node.organization.id) }) }}
              </NuxtLink>
              <p
                v-else
                class="org-dataset-count"
              >
                {{ t('message.organizations.datasets_count', { count: getDatasetCount(node.organization.id) }) }}
              </p>
            </div>
          </div>
        </div>
        <div class="buttons">
          <div>
            <OdsButton
              v-if="node.children.length > 0"
              variant="link"
              :title="isNodeExpanded(node.id) ? t('message.dataset_search.hide_filters') : t('message.dataset_search.show_filters')"
              :aria-label="isNodeExpanded(node.id) ? t('message.dataset_search.hide_filters') : t('message.dataset_search.show_filters')"
              size="sm"
              @click="toggleNode(node.id)"
            >
              <template #icon>
                <SvgIcon
                  icon="ChevronDown"
                  role="btn"
                  :class="{ rotated: isNodeExpanded(node.id) }"
                />
              </template>
              {{ node.children.length + " " + t('message.organizations.sub_organizations') }}
            </OdsButton>
          </div>
        </div>
      </div>
      <Transition name="expand">
        <div
          v-if="node.children.length > 0"
          v-show="isNodeExpanded(node.id)"
          class="organization-tree__children"
        >
          <OdsOrganizationListItem
            :nodes="node.children"
            :dataset-count-by-organization-id="props.datasetCountByOrganizationId"
          />
        </div>
      </Transition>
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

.organization-tree__children {
  overflow: hidden;
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

.column {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  .row {
    display: flex;
    flex-direction: row;
    justify-content: flex-start;
    align-items: center;
    gap: 12px;
    .avatar {
      height: 48px;
      width: 48px;
      background-color: grey;
      display: flex;
      flex-direction: row;
      align-items: center;
      justify-content: center;
    }
    .org-props {
      display: flex;
      flex-direction: column;
    }
  }
  .buttons {
    display: flex;
    flex-direction: row;
    justify-content: flex-end;
    width: 100%;
  }
}
.rotated {
  transform: rotate(180deg);
  transition: transform 0.2s;
}

.expand-enter-active,
.expand-leave-active {
  transition: max-height 0.28s ease, opacity 0.2s ease, transform 0.22s ease;
}

.expand-enter-from,
.expand-leave-to {
  max-height: 0;
  opacity: 0;
  transform: translateY(-4px);
}

.expand-enter-to,
.expand-leave-from {
  max-height: 1200px;
  opacity: 1;
  transform: translateY(0);
}

@media (prefers-reduced-motion: reduce) {
  .expand-enter-active,
  .expand-leave-active,
  .rotated {
    transition: none;
  }
}
</style>
