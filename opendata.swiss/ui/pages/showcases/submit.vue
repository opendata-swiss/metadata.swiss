<template>
  <OdsPage :page="{ title: 'New Showcase' }">
    <template #header>
      <OdsNotificationBanner :open="success === true" type="success">
        The showcase has been submitted

        <template #buttons>
          <OdsButton variant="outline" title="Close" icon-right icon="Checkmark" @click="closeMessages"/>
        </template>
      </OdsNotificationBanner>
      <OdsNotificationBanner :open="success === false" type="error">
        Failed to submit showcase.

        <pre>{{ submissionError }}</pre>
        <ul>
          <li v-for="issue in submissionValidationIssues" :key="issue.path.join('-')">
            {{ issue.path.join('.')}}: {{ issue.message }}
          </li>
        </ul>

        <template #buttons>
          <OdsButton variant="outline" title="Close" icon-right icon="Checkmark" @click="closeMessages"/>
        </template>
      </OdsNotificationBanner>
    </template>

    <section class="section section--py">
      <div class="container">
        <ClientOnly>
          <form ref="newShowcaseForm" class="form" method="post" @submit="submit">
            <div class="form__group__input">
              <OdsInput id="title-de" label="Title (DE)" placeholder="Titel auf Deutsch" required />
              <OdsInput id="title-fr" label="Title (FR)" placeholder="Titre en Français" required />
              <OdsInput id="title-it" label="Title (IT)" placeholder="Titolo in Italiano" required />
              <OdsInput id="title-en" label="Title (EN)" placeholder="Title in English" required />
            </div>
            <div class="form__group__input">
              <OdsInput id="image" type="file" label="Image" accept="image/*" required />
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
              <OdsTextarea id="body-de" label="Body (DE)" placeholder="Beschreibung auf Deutsch" required />
              <OdsTextarea id="body-fr" label="Body (FR)" placeholder="Description en Français" required />
              <OdsTextarea id="body-it" label="Body (IT)" placeholder="Descrizione in Italiano" required />
              <OdsTextarea id="body-en" label="Body (EN)" placeholder="Description in English" required />
            </div>
            <div class="form__group">
              <OdsButton submit variant="outline-negative" title="Submit" />
            </div>
          </form>
        </ClientOnly>
      </div>
    </section>
  </OdsPage>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import type { $ZodIssue as ZodIssue } from "zod/v4/core";
import OdsMultiSelect from "../../app/components/dataset/OdsMultiSelect.vue";
import {useVocabularySearch} from "../../app/piveau/search";
import OdsNotificationBanner from "../../app/components/OdsNotificationBanner.vue";
import OdsTextarea from "../../app/components/OdsTextarea.vue";
import OdsButton from "../../app/components/OdsButton.vue";
import OdsInput from "../../app/components/OdsInput.vue";
import OdsSelect from "../../app/components/OdsSelect.vue";
import OdsPage from "../../app/components/OdsPage.vue";

const { locale } = useI18n()

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
const submissionValidationIssues = ref<ZodIssue[]>([])

const newShowcaseForm = ref<HTMLFormElement | null>(null)
async function submit(e: Event) {
  e.preventDefault()

  try {
    const response = await fetch('/api/showcases', {
      method: 'POST',
      body: new FormData(newShowcaseForm.value!),
      headers: {
        'Accept-Language': locale.value,
      }
    })
    if (response.ok) {
      success.value = true
      if (newShowcaseForm.value) {
        newShowcaseForm.value.reset()
      }
    } else if (response.status === 400) {
      submissionError.value = 'Form contains invalid data:'
      submissionValidationIssues.value = await response.json()
      success.value = false
    } else {
      submissionError.value = `Server responded with: ${response.status} - ${response.statusText}`
      success.value = false
    }
  } catch (e) {
    submissionError.value = `${e.message}\n${e.stack}`
    success.value = false
  } finally {
    window.scrollTo({top: 0, behavior: 'smooth'})
  }
}

function closeMessages() {
  success.value = null
  submissionError.value = null
}
</script>
