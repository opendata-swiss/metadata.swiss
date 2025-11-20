import { defineCollection, defineContentConfig } from '@nuxt/content'
import { z } from 'zod/v4'
import * as showcaseSchema from "./src/schema/showcase.js";

export default defineContentConfig({
  collections: {
    pages: defineCollection({
      source: 'pages/*.md',
      type: 'page',
      schema: z.object({
        title: z.string(),
        heading: z.string().optional(),
        subHeading: z.string().optional(),
        permalink: z.string().optional(),
      })
    }),
    handbook: defineCollection({
      source: 'handbook/**/*.md',
      type: 'page',
      schema: z.object({
        title: z.string(),
        breadcrumb_title: z.string(),
        permalink: z.string(),
        section: z.string(),
      })
    }),
    handbookSections: defineCollection({
      source: 'sections/*.md',
      type: 'page',
      schema: z.object({
        id: z.string(),
        title: z.string(),
      })
    }),
    blog: defineCollection({
      source: 'blog/*.md',
      type: 'page',
      schema: z.object({
        title: z.string(),
        slug: z.string().optional(),
        date: z.date().optional(),
        subHeading: z.string().optional(),
      })
    }),
    showcases: defineCollection({
      source: 'showcases/*.md',
      type: 'page',
      schema: z.object(showcaseSchema.shape),
    })
  }
})
