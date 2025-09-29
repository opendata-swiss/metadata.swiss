<template>
  <NuxtTime
        :datetime="props.date"
        :locale="locale"
        :relative="!showRaw"
        :title="showRaw ? t('message.dataset_detail.show_relative') : t('message.dataset_detail.show_raw')"
        :aria-label="showRaw ? t('message.dataset_detail.show_relative') : t('message.dataset_detail.show_raw')"
        :hour="showRaw ? '2-digit' : undefined"
        :minute="showRaw ? '2-digit' : undefined"
        :second="showRaw ? '2-digit' : undefined"
        :year="'numeric'"
        :month="'2-digit'"
        :day="'2-digit'"
        class="date-toggle"
        @click="toggle($event)"
      />
</template>


<script setup lang="ts">

import { useI18n } from 'vue-i18n'

const { locale, t } = useI18n()
const showRaw = ref(false)

const props = defineProps({
  date: {
    type: Date,
    required: true
  }
});

function toggle(event: PointerEvent) {
  event.preventDefault()
  event.stopPropagation()
  showRaw.value = !showRaw.value
}

</script>

<style lang="scss" scoped>

.date-toggle {
  cursor: pointer;
}

</style>
