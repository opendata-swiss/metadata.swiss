// server/middleware/legacyRedirect.ts
import type { H3Event } from 'h3'

export default defineEventHandler((event: H3Event) => {
  const url = event.req.url || ''
  const match = url.match(/^\/([a-z]{2})\/perma\/(.+)$/)
  if (match) {
    const lang = match[1]
    const datasetId = match[2]
    const newId = datasetId.replace('@', '-')
    const newPath = `/${lang}/dataset/${newId}`

    // Permanent redirect
    event.res.statusCode = 301
    event.res.setHeader('Location', newPath)
    event.res.end()
  }
})
