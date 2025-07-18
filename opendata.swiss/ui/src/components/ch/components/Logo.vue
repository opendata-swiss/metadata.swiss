<template>
  <a class="logo" :href="link">

    <img
      v-if="isFreebrand"
      class="logo__name ods-logo-horizontal"
      role="img"
      aria-hidden="true"
      src="@/assets/ods/logo/logo_horizontal.png"
    />
    <img
      v-if="isFreebrand"
      class="logo__flag ods-logo-small"
      role="img"
      aria-hidden="true"
      src="@/assets/ods/logo/favicon-128.png"
    />
    <div class="logo__separator" role="separator" aria-hidden="true" />
    <div class="logo-title__container">
      <div class="logo__accronym" v-html="accronym" />
      <div class="logo__title">
        <div v-html="title" />
        <div v-if="isEasyLanguage" class="badge-easy-language">
          Inhalte in Leichter Sprache
        </div>
        <div v-if="isSignLanguage" class="badge-sign-language">
          Inhalte in Gebärdensprache
        </div>
      </div>
    </div>
  </a>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps({
  title: {
    type: String,
    required: false,
    default: () => '',
  },
  accronym: {
    type: String,
    required: false,
    default: () => '',
  },
  link: {
    type: String,
    required: false,
    default: () => '/',
  },
  isFreebrand: {
    type: Boolean,
    default: () => false,
  },
  isEasyLanguage: {
    type: Boolean,
    default: () => false,
  },
  isSignLanguage: {
    type: Boolean,
    default: () => false,
  },
})


</script>


<style lang="scss" scoped>
.ods-logo-horizontal {
  height: 30px;
  width: 244px;
}

.ods-logo-small {
  height: 44px;
  width: 44px;
}


@media (min-width: 1280px) {
    .ods-logo-small {
       display: none !important;
    }
}
</style>
