<template>
  <OdsPage>
    <main id="main-content">
      <!-- search panel -->
      <section
        id="search-results"
        class="section section--default"
      >
        <div class="container gap--responsive">
          <h2 class="h2">
            Fine-tune you subscription preferences
          </h2>
          <section>
            <form ref="form">
              <OdsRadioGroup legend="How often do you want to receive updates?">
                <OdsRadio
                  id="daily"
                  name="frequency"
                  value="daily"
                  label="Daily"
                  :checked="frequency === 'daily'"
                />
                <OdsRadio
                  id="weekly"
                  name="frequency"
                  value="weekly"
                  label="Weekly"
                  :checked="frequency === 'weekly'"
                />
              </OdsRadioGroup>

              <OdsFormField
                v-if="datasets.length > 0"
                label="Datasets selected for updates"
                for="dataset"
              >
                <OdsCheckbox
                  v-for="dataset in datasets"
                  :key="dataset.id"
                  name="dataset"
                  :value="dataset.id"
                  :label="dataset.name"
                  checked
                />
              </OdsFormField>

              <OdsFormField
                v-if="categories.length > 0"
                label="Categories selected for updates"
                for="dataset"
              >
                <OdsCheckbox
                  v-for="category in categories"
                  :key="category.id"
                  name="category"
                  :value="category.id"
                  :label="category.pref_label"
                  checked
                />
              </OdsFormField>
              <OdsButton
                title="Update your preferences"
                variant="outline"
                @click="updatePreferences"
              />
            </form>
          </section>
          <section>
            <h2 class="h2">
              Or unsubscribe completely
            </h2>
            <OdsButton
              title="Unsubscribe from all emails"
              variant="outline"
            />
          </section>
        </div>
      </section>
    </main>
  </OdsPage>
</template>

<script setup lang="ts">
import OdsPage from '../../app/components/OdsPage.vue'
import OdsRadioGroup from '../../app/components/OdsRadioGroup.vue'
import OdsRadio from '../../app/components/OdsRadio.vue'
import OdsCheckbox from '../../app/components/OdsCheckbox.vue'
import OdsFormField from '../../app/components/OdsFormField.vue'
import { ref } from 'vue'
import { useRoute } from 'vue-router'
import { useDatasetsSearch } from '../../app/piveau/datasets.js'
import OdsButton from '../../app/components/OdsButton.vue'
import { useVocabularySearch } from '../../app/piveau/vocabularies'

const route = useRoute()

const form = ref<HTMLFormElement>()
const datasets = ref([])
const categories = ref([])
const frequency = ref()

const query = {
  id: route.query.id,
  token: route.query.token,
}
const { data } = await useFetch('/api/subscription/preferences', { query })
const preferences = data.value.preferences

const { useResource } = useDatasetsSearch()
const { useSearch } = useVocabularySearch()
const vocabSearch = useSearch({
  queryParams: {
    limit: 100,
    vocabulary: 'data-theme',
  },
})

frequency.value = preferences.frequency || 'daily'

const loadCategories = (preferences.categories || []).map(async (category) => {
  await vocabSearch.query.suspense()
  return vocabSearch.getSearchResultsEnhanced.value
    .filter(result => result.id === category)
    .shift()
})
categories.value = (await Promise.all(loadCategories)).filter(Boolean)

const datasetsLoaded = preferences.datasets.map(async (id) => {
  const { query, resultEnhanced } = useResource(id)
  await query.suspense()
  return {
    id,
    name: resultEnhanced.value.getTitle,
  }
})

datasets.value = (await Promise.all(datasetsLoaded)).sort((a, b) => a.name.localeCompare(b.name))

async function updatePreferences() {
  const { data, error } = await useFetch('/api/subscription/preferences', {
    query,
    method: 'PUT',
    body: new URLSearchParams(new FormData(form.value!) as unknown as Array<[string, string]>),
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
  })

  if (error) {
    alert('Preferences updated successfully')
  }
  else {
    console.error(data)
    alert('Failed to update preferences. Please try again later.')
  }
}
</script>

<style scoped>
  .form-group, section+section {
    padding-top: 30px
  }
</style>
