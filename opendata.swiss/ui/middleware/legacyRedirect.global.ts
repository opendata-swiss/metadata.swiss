import { defineNuxtRouteMiddleware, navigateTo } from "nuxt/app"

export default defineNuxtRouteMiddleware((to) => {
  const match = to.path.match(/^\/([a-z]{2})\/perma\/(.+)$/)
  if (match) {
    const lang = match[1]
    const datasetId = match[2]

    // Replace only the first "@"
    const newId = datasetId.replace('@', '-')

    return navigateTo(`/${lang}/dataset/${newId}`, { redirectCode: 301 })
  }
})
