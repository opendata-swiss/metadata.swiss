import { defineEventHandler, getRequestURL } from 'h3'

export default defineEventHandler((event) => {
  const url = getRequestURL(event)
  const pathname = url.pathname
  const defaultLocale = 'de'
  const supportedLocales = ['en', 'fr', 'de', 'it']

  // Skip Nuxt internal paths and static files
  if (
    pathname.startsWith('/__nuxt') || // Nuxt internal
    pathname.startsWith('/_nuxt') ||  // Sometimes _nuxt
    pathname.startsWith('/favicon.ico') ||
    pathname.startsWith('/robots.txt') ||
    pathname.startsWith('/api') ||     // Your API routes
    pathname.includes('.')              // Static files like .js, .css
  ) {
    return
  }

  // Extract first segment of the path
  const firstSegment = pathname.split('/').filter(Boolean)[0]

  // If first segment is not a supported locale, redirect to default locale
  if (!supportedLocales.includes(firstSegment)) {
    const redirectUrl = `/${defaultLocale}${pathname}${url.search}`
    event.res.writeHead(302, { Location: redirectUrl })
    event.res.end()
  }
})
