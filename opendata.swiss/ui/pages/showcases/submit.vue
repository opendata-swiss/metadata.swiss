<template>
  <OdsPage :page="{ title }">
    <template #header>
      <OdsNotificationBanner :open="success === true" type="success">
        {{ t('success_message') }}

        <template #buttons>
          <OdsButton variant="outline" title="Close" icon-right icon="Checkmark" @click="closeMessages"/>
        </template>
      </OdsNotificationBanner>
      <OdsNotificationBanner :open="success === false" type="error">
        {{ t('failure_message') }}

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
            <OdsTab :title="`${t('group_german')}`">
              <div class="form__group">
                <OdsInput id="title[de]" :label="t('field_title')" />
                <ToastMarkdownEditor id="body[de]" :label="t('field_body')" />
              </div>
            </OdsTab>
            <OdsTab :title="`${t('group_french')}`">
              <div class="form__group">
                <OdsInput id="title[fr]" :label="t('field_title')" />
                <ToastMarkdownEditor id="body[fr]" :label="t('field_body')" />
              </div>
            </OdsTab>
            <OdsTab :title="`${t('group_italian')}`">
              <div class="form__group">
                <OdsInput id="title[it]" :label="t('field_title')" />
                <ToastMarkdownEditor id="body[it]" :label="t('field_body')" />
              </div>
            </OdsTab>
            <OdsTab :title="`${t('group_english')}`">
              <div class="form__group">
                <OdsInput id="title[en]" :label="t('field_title')" />
                <ToastMarkdownEditor id="body[en]" :label="t('field_body')" />
              </div>
            </OdsTab>
            <OdsTab :title="`${t('group_general')} *`">
              <div class="form__group">
                <OdsSelect id="type" name="type" :label="t('field_type')" required>
                  <option v-for="type in showcaseType" :key="type.id" :value="type.id">
                    {{ type.title }}
                  </option>
                </OdsSelect>
                <div class="form__group">
                  <OdsInput id="image" type="file" :label="t('field_image')" accept="image/*" required />
                </div>
                <OdsInput id="url" :label="t('field_url')" />
                <div class="form__group">
                  <OdsMultiSelect
                    id="datasets"
                    :label="t('field_datasets.label')"
                    :load-options="searchDatasets"
                    :close-on-select="false"
                    :options="datasets"
                  >
                    <template #no-options>
                      {{ t('field_datasets.prompt') }}
                    </template>
                    <template #selected-option="option" >
                      {{ option.title }}
                      <input type="hidden" :name="`datasets[${option.id}]`" :value="option.title">
                    </template>
                  </OdsMultiSelect>
                </div>
                <OdsMultiSelect :label="t('field_categories.label')" :options="dataThemes" :close-on-select="false">
                  <template #no-options>
                    {{ t('field_categories.prompt') }}
                  </template>
                  <template #selected-option="option" >
                    {{ option.title }}
                    <input type="hidden" name="categories" :value="option.id">
                  </template>
                </OdsMultiSelect>
                <OdsInput id="tags" :label="t('field_tags.label')" :placeholder="t('field_tags.prompt')" />
              </div>
            </OdsTab>
          </OdsTabs>
          <div class="form__group">
            <OdsButton
              submit
              variant="outline-negative"
              :title="t('submit_button')"
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
import { reactive, ref, toRefs } from 'vue'
import type { $ZodIssue as ZodIssue } from 'zod/v4/core'
import type { SearchParamsBase } from '@piveau/sdk-core'
import { debounce } from 'perfect-debounce'
import OdsMultiSelect from '../../app/components/dataset/OdsMultiSelect.vue'
import { useDatasetsSearch } from '../../app/piveau/datasets'
import { useVocabularySearch } from '../../app/piveau/vocabularies'
import OdsNotificationBanner from '../../app/components/OdsNotificationBanner.vue'
import OdsButton from '../../app/components/OdsButton.vue'
import OdsInput from '../../app/components/OdsInput.vue'
import OdsSelect from '../../app/components/OdsSelect.vue'
import OdsPage from '../../app/components/OdsPage.vue'
import SvgIcon from '../../app/components/SvgIcon.vue'
import ToastMarkdownEditor from '../../app/components/ToastMarkdownEditor.vue'
import OdsTabs from '../../app/components/OdsTabs.vue'
import OdsTab from '../../app/components/OdsTab.vue'

const i18n = useI18n()
const { locale } = i18n
const t = (key: string) => i18n.t(`message.showcase.submission_form.${key}`)

const { useSearch } = useVocabularySearch()
const searchDataThemes = useSearch({
  queryParams: {
    limit: 100,
    vocabulary: 'data-theme',
  },
})

const dataThemes = computed(() => {
  return searchDataThemes.getSearchResultsEnhanced.value.map(item => ({
    id: item.resource,
    title: item.pref_label,
  }))
})

const searchShowcaseTypes = useSearch({
  queryParams: {
    vocabulary: 'showcase-types',
  },
})

const showcaseType = computed(() => {
  return searchShowcaseTypes.getSearchResultsEnhanced.value.map(item => ({
    id: item.resource,
    title: item.pref_label,
  }))
})

const title = 'New Showcase'
useSeoMeta({ title: `${title} | opendata.swiss` })

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
      },
    })
    if (response.ok) {
      success.value = true
      newShowcaseForm.value?.reset()
    }
    else if (response.status === 400 || response.status === 409) {
      submissionError.value = 'Form contains invalid data:'
      submissionValidationIssues.value = await response.json()
      success.value = false
    }
    else {
      submissionError.value = `Server responded with: ${response.status} - ${response.statusText}`
      success.value = false
    }
  }
  catch (e) {
    submissionError.value = `${e.message}\n${e.stack}`
    success.value = false
  }
  finally {
    window.scrollTo({ top: 0, behavior: 'smooth' })
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
  sort: 'relevance',
})
const { useSearch: datasetSearch } = useDatasetsSearch()
const { query, getSearchResultsEnhanced } = datasetSearch({
  queryParams: toRefs(datasetQueryParams),
})

const datasets = ref([] as { id: string, title: string }[])
const searchDatasets = debounce(async function (arg: string, loading: (arg: boolean) => void) {
  if (arg.length === 0) {
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
</style>
