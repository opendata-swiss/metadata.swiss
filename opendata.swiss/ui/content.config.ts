import { defineCollection, defineContentConfig } from '@nuxt/content'
import { z } from 'zod/v4'
import * as showcaseSchema from './src/schema/showcase.js'

function sourcesFor(include: string) {
  const sources = [{
    include,
  }]

  if (process.env.NODE_ENV === 'development') {
    // During development, include test sources
    sources.push({
      include: `.test/${include}`,
    })
    sources.push({
      include: `.local/${include}`,
    })
  }

  return sources
}

export default defineContentConfig({
  collections: {
    pages: defineCollection({
      source: sourcesFor('pages/*.md'),
      type: 'page',
      schema: z.object({
        title: z.string(),
        heading: z.string().optional(),
        subHeading: z.string().optional(),
        permalink: z.string().optional(),
        rawbody: z.string(),
      }),
    }),
    handbook: defineCollection({
      source: sourcesFor('handbook/**/*.md'),
      type: 'page',
      schema: z.object({
        title: z.string(),
        breadcrumb_title: z.string(),
        permalink: z.string(),
        section: z.string(),
      }),
    }),
    handbookSections: defineCollection({
      source: sourcesFor('sections/*.md'),
      type: 'page',
      schema: z.object({
        id: z.string(),
        title: z.string(),
      }),
    }),
    blog: defineCollection({
      source: sourcesFor('blog/*.md'),
      type: 'page',
      schema: z.object({
        title: z.string(),
        slug: z.string().optional(),
        date: z.date().optional(),
        subHeading: z.string().optional(),
      }),
    }),
    showcases: defineCollection({
      source: sourcesFor('showcases/*.md'),
      type: 'page',
      schema: z.object(showcaseSchema.shape),
    }),
  },
})
