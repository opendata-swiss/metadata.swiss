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

function login() {
  window.location.href = '/api/auth/keycloak'
}
</script>
