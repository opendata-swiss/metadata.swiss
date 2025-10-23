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
        <ul v-if="Array.isArray(submissionValidationIssues)">
          <li v-for="issue in submissionValidationIssues" :key="issue.path.join('-')">
            {{ issue.path.join('.')}}: {{ issue.message }}
          </li>
        </ul>
        <p v-else>
          {{ submissionValidationIssues.error }}
        </p>

        <template #buttons>
          <OdsButton variant="outline" title="Close" icon-right icon="Checkmark" @click="closeMessages"/>
        </template>
      </OdsNotificationBanner>
    </template>

    <section class="section section--py">
      <div class="container">
          <form ref="newShowcaseForm" class="form" method="post" @submit="submit">
            <OdsTabs>
              <OdsTab title="German *">
                <div class="form__group">
                  <OdsInput id="title[de]" label="Title (DE)" placeholder="Titel auf Deutsch" required />
                  <ToastMarkdownEditor id="body[de]" label="Body (DE)" required />
                </div>
              </OdsTab>
              <OdsTab title="French *">
                <div class="form__group">
                  <OdsInput id="title[fr]" label="Title (FR)" placeholder="Titre en Français" required />
                  <ToastMarkdownEditor id="body[fr]" label="Body (FR)" required />
                </div>
              </OdsTab>
              <OdsTab title="Italian *">
                <div class="form__group">
                  <OdsInput id="title[it]" label="Title (IT)" placeholder="Titolo in Italiano" required />
                  <ToastMarkdownEditor id="body[it]" label="Body (IT)" required />
                </div>
              </OdsTab>
              <OdsTab title="English *">
                <div class="form__group">
                  <OdsInput id="title[en]" label="Title (EN)" placeholder="Title in English" required />
                  <ToastMarkdownEditor id="body[en]" label="Body (EN)" required />
                </div>
              </OdsTab>
              <OdsTab title="General *">
                <div class="form__group">
                  <OdsSelect id="type" name="type" label="Type" required>
                    <option value="application">Application</option>
                    <option value="data_visualization">Data Visualization</option>
                    <option value="event">Event</option>
                    <option value="blog_and_media_articles">Blog/Article</option>
                  </OdsSelect>
                  <div class="form__group">
                    <OdsInput id="image" type="file" label="Image" accept="image/*" required />
                  </div>
                  <OdsInput id="url" label="Website" />
                  <div class="form__group">
                    <OdsMultiSelect
                      id="datasets"
                      label="Datasets"
                      :load-options="searchDatasets"
                      :close-on-select="false"
                      :options="datasets"
                    >
                      <template #no-options>
                        type to search datasets...
                      </template>
                      <template #selected-option="option" >
                        {{ option.title }}
                        <input type="hidden" :name="`datasets[${option.id}]`" :value="option.title">
                      </template>
                    </OdsMultiSelect>
                  </div>
                  <OdsMultiSelect label="Categories" :options="dataThemes" :close-on-select="false">
                    <template #no-options>
                      type to search categories...
                    </template>
                    <template #selected-option="option" >
                      {{ option.title }}
                      <input type="hidden" name="categories" :value="option.id">
                    </template>
                  </OdsMultiSelect>
                  <OdsInput id="tags" label="Tags" placeholder="Enter tags separated by commas" />
                </div>
              </OdsTab>
            </OdsTabs>
            <div class="form__group">
              <OdsButton
                submit
                variant="outline-negative"
                title="Submit"
                icon-right
                :style="submitting ? 'pointer-events: none; cursor: wait' : ''"
              >
                <template #icon>
                  <SvgIcon v-if="submitting" icon="Spinner" class="btn__icon btn__icon--spin"  />
                  <SvgIcon v-else icon="Checkmark" class="btn__icon"/>
                </template>
              </OdsButton>
            </div>
          </form>
      </div>
    </section>
  </OdsPage>
</template>

<script setup lang="ts">
import {reactive, ref, toRefs} from 'vue'
import type { $ZodIssue as ZodIssue } from "zod/v4/core";
import type { SearchParamsBase } from '@piveau/sdk-core'
import {debounce} from 'perfect-debounce'
import OdsMultiSelect from "../../app/components/dataset/OdsMultiSelect.vue";
import {useDatasetsSearch, useVocabularySearch} from "../../app/piveau/search";
import OdsNotificationBanner from "../../app/components/OdsNotificationBanner.vue";
import OdsButton from "../../app/components/OdsButton.vue";
import OdsInput from "../../app/components/OdsInput.vue";
import OdsSelect from "../../app/components/OdsSelect.vue";
import OdsPage from "../../app/components/OdsPage.vue";
import SvgIcon from "../../app/components/SvgIcon.vue";
import ToastMarkdownEditor from "../../app/components/ToastMarkdownEditor.vue";
import OdsTabs from "../../app/components/OdsTabs.vue";
import OdsTab from "../../app/components/OdsTab.vue";

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

const submitting = ref(false)
const success = ref<boolean | null>(null)
const submissionError = ref<string | null>(null)
const submissionValidationIssues = ref<{ error: string } | ZodIssue[]>([])

const newShowcaseForm = ref<HTMLFormElement | null>(null)
async function submit(e: Event) {
  e.preventDefault()

  if (submitting.value) {
    return
  }

  try {
    submitting.value = true
    const response = await fetch('/api/showcases', {
      method: 'POST',
      body: new FormData(newShowcaseForm.value!),
      headers: {
        'Accept-Language': locale.value,
      }
    })
    if (response.ok) {
      success.value = true
      newShowcaseForm.value?.reset()
    } else if (response.status === 400 || response.status === 409) {
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
    submitting.value = false
  }
}

function closeMessages() {
  success.value = null
  submissionError.value = null
}

const searchTerm = ref()
const datasetQueryParams: SearchParamsBase = reactive({
  limit: 10,
  q: searchTerm,
  sort: 'relevance'
})
const { useSearch: datasetSearch } = useDatasetsSearch()
const { query, getSearchResultsEnhanced } = datasetSearch({
  queryParams: toRefs(datasetQueryParams)
})

const datasets = ref([] as { id: string; title: string }[])
const searchDatasets = debounce(async function (arg: string, loading: (arg: boolean) => void) {
  if(arg.length === 0) {
    return
  }

  loading(true)
  searchTerm.value = arg
  await query.suspense()
  datasets.value = getSearchResultsEnhanced.value.map(dataset => ({
    id: dataset.getId,
    title: dataset.getTitle,
  }))
  loading(false)
}, 300)
</script>

<style scoped>
.btn__icon--spin {
  animation: spin 1s linear infinite;
}
@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.tab__container {
  color: red;
}
</style>
