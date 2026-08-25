<script setup lang="ts">
const localePath = useLocalePath()

await navigateTo(localePath('/organizations'), {
  redirectCode: 301,
})
</script>
