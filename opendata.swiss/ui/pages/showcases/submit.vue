<template>
  <OdsPage :page="{ title: 'New Showcase' }">
    <template #header>
      <OdsNotificationBanner :open="success === true" type="success">
        Your showcase has been submitted

        <template #buttons>
          <OdsButton variant="outline" title="Close" icon-right icon="Checkmark" @click="closeMessages"></OdsButton>
        </template>
      </OdsNotificationBanner>
      <OdsNotificationBanner :open="success === false" type="error">
        Failed to submit showcase.

        <pre>{{ submissionError }}</pre>

        <template #buttons>
          <OdsButton variant="outline" title="Close" icon-right icon="Checkmark" @click="closeMessages"></OdsButton>
        </template>
      </OdsNotificationBanner>
    </template>

    <section class="section section--py">
      <div class="container">
        <ClientOnly>
          <form method="post" ref="newShowcaseForm" action="/api/new-showcase">
            <div class="form__group__input">
              <OdsInput id="title-de" label="Title (DE)" placeholder="Titel auf Deutsch"/>
              <OdsInput id="title-fr" label="Title (FR)" placeholder="Titre en Français"/>
              <OdsInput id="title-it" label="Title (IT)" placeholder="Titolo in Italiano"/>
              <OdsInput id="title-en" label="Title (EN)" placeholder="Title in English"/>
            </div>
            <div class="form__group">
              <OdsInput id="url" label="Website" />
              <OdsMultiSelect id="categories" name="categories" label="Categories" :options="dataThemes" />
              <OdsSelect id="type" name="type" label="Type" required>
                <option value="application">Application</option>
                <option value="data_visualization">Data Visualization</option>
                <option value="event">Event</option>
                <option value="blog_and_media_articles">Blog/Article</option>
              </OdsSelect>
              <OdsInput id="tags" label="Tags" />
            </div>
            <div class="form__group">
              <OdsTextarea id="body-de" label="Body (DE)" placeholder="Beschreibung auf Deutsch" />
              <OdsTextarea id="body-fr" label="Body (FR)" placeholder="Description en Français" />
              <OdsTextarea id="body-it" label="Body (IT)" placeholder="Descrizione in Italiano" />
              <OdsTextarea id="body-en" label="Body (EN)" placeholder="Description in English" />
            </div>
            <div class="form__group">
              <OdsButton variant="outline-negative" title="Submit" @click="submit"></OdsButton>
            </div>
          </form>
        </ClientOnly>
      </div>
    </section>
  </OdsPage>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import OdsMultiSelect from "../../app/components/dataset/OdsMultiSelect.vue";
import {useVocabularySearch} from "../../app/piveau/search";
import OdsNotificationBanner from "../../app/components/OdsNotificationBanner.vue";

const { useSearch } = useVocabularySearch()
const search = useSearch({
  queryParams: {
    vocabulary: 'data-theme',
  }
})

const dataThemes = computed(() => {
  return search.getSearchResultsEnhanced.value.map(item => ({
    id: item.resource,
    title: item.pref_label
  }))
})

useSeoMeta({title: 'New Showcase | opendata.swiss'})

const success = ref<boolean | null>(null)
const submissionError = ref<string | null>(null)

const newShowcaseForm = ref<HTMLFormElement | null>(null)
function submit() {
  fetch('/api/showcases', {
    method: 'POST',
    body: new FormData(newShowcaseForm.value!)
  }).then(response => {
    if (response.ok) {
      success.value = true
      if (newShowcaseForm.value) {
        newShowcaseForm.value.reset()
      }
    } else {
      submissionError.value = `Server responded with: ${response.status} - ${response.statusText}`
      success.value = false
    }
  }).catch((e) => {
    submissionError.value = `${e.message}\n${e.stack}`
    success.value = false
  }).finally(() => {
    window.scrollTo({ top: 0, behavior: 'smooth' })
  })
}

function closeMessages() {
  success.value = null
  submissionError.value = null
}
</script>
