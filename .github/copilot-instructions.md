
# Copilot Instructions for metadata.swiss

- **Nuxt UI**:
	- Use `<script setup lang="ts">` and the Composition API.
	- Prefer composables, auto-imported components, and Nuxt features (pages, layouts, middleware).
	- Use SCSS for styling, following the design system.
	- Avoid hardcoding; use config files and environment variables.
	- Props should use `defineProps` with explicit types.
	- Data fetching: Use Nuxt's `useFetch` and `useAsyncData`.
    - We use Nuxt 4 with TypeScript