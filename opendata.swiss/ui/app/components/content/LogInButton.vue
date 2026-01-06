<template>
  <ods-button
    variant="outline"
    @click="login"
  >
    <slot />
  </ods-button>
</template>

<script setup lang="ts">
import OdsButton from '~/components/OdsButton.vue'
import { useLogin } from '@/composables/login'

const login = useLogin()
</script>
