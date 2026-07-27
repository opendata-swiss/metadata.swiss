<script setup lang="ts">
import { useI18n } from '#imports'

import OdsCard from '~/components/content/OdsCard.vue'

const { id, label, description } = defineProps<{
  id: string
  label: string
  description?: string
}>()

const { locale } = useI18n()
const organizationPath = computed(() => `/${locale.value}/organizations/${encodeURIComponent(id)}`)
</script>

<template>
  <a
    :href="organizationPath"
    class="organization-card-link"
  >
    <OdsCard
      :title="label"
      type="list"
      clickable
    >
      <p v-if="description">
        {{ description }}
      </p>
    </OdsCard>
  </a>
</template>

<style scoped>
.organization-card-link {
  color: inherit;
  display: block;
  text-decoration: none;
}
</style>
