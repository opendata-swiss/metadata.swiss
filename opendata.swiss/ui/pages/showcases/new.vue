<template>
  <OdsPage :page="{ title: 'New Showcase' }">
    <section class="section section--py">
      <div class="container">
        <ClientOnly>
          <form method="post" ref="newShowcaseForm">
            <div class="form__group__input">
              <OdsInput id="title-de" label="Title (DE)" placeholder="Titel auf Deutsch"/>
              <OdsInput id="title-fr" label="Title (FR)" placeholder="Titre en Français"/>
              <OdsInput id="title-it" label="Title (IT)" placeholder="Titolo in Italiano"/>
              <OdsInput id="title-en" label="Title (EN)" placeholder="Title in English"/>
            </div>
            <div class="form__group">
              <OdsInput id="website" label="Website" />
              <OdsMultiSelect id="category" name="category" label="Categories" :options="dataThemes" />
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

const { useSearch } = useVocabularySearch()
const search = useSearch({
  queryParams: {
    vocabulary: 'data-theme',
  }
})

const dataThemes = computed(() => {
  return search.getSearchResultsEnhanced.value.map(item => ({
    id: item.id,
    title: item.pref_label
  }))
})

useSeoMeta({title: 'New Showcase | opendata.swiss'})

const newShowcaseForm = ref<HTMLFormElement | null>(null)
function submit() {
  console.dir(newShowcaseForm.value)
}
</script>
