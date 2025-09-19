<template>
    <div class="ods-license">
      <h2 class="download-item__title">{{ t(`message.terms_of_use.ods_${termsName}.title`) }}</h2>
      <img
        :src="imageSrc"
        :alt="t(`message.terms_of_use.ods_${termsName}.title`)"
        :title="t(`message.terms_of_use.ods_${termsName}.title`)">
    </div>

  <ul>
    <li>{{ t(`message.terms_of_use.ods_${termsName}.condition_1`) }}</li>
    <li>{{ t(`message.terms_of_use.ods_${termsName}.condition_2`) }}</li>
    <li>{{ t(`message.terms_of_use.ods_${termsName}.condition_3`) }}</li>
  </ul>
</template>

<script setup lang="ts">
import {computed} from 'vue';
import { useI18n } from 'vue-i18n';

const { t } = useI18n();

const props = defineProps({
  name: {
    type: String,
    required: true
  }
})

const termsName = computed(() => {
  const name = props.name;
  switch (name) {
    case 'http://dcat-ap.ch/vocabulary/licenses/terms_by':
      return 'by';
    case 'http://dcat-ap.ch/vocabulary/licenses/terms_open':
      return 'open';
    case 'http://dcat-ap.ch/vocabulary/licenses/terms_ask':
      return 'ask';
    case'http://dcat-ap.ch/vocabulary/licenses/terms_by_ask':
      return 'by_ask';
    default:
      return props.name;
  }

});

const imageSrc = computed(() => `/img/terms-of-use/terms_${termsName.value}.png`);
</script>

<style lang="scss" scoped>
.ods-license {
  display: flex;
  justify-content: flex-start;
  align-items: flex-start;
  flex-direction: column;

}

img {
    height: 100%;

}

ul {
  margin-top: 24px;
  margin-left: 0;
  padding: 0 !important;
}

li {
  list-style-type: none;
  margin-bottom: 0;
  margin-top: 44px !important;
}
</style>
