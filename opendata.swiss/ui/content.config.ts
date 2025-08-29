import { defineCollection, defineContentConfig, z } from '@nuxt/content'

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
      schema: z.object({
        active: z.boolean(),
        title: z.string(),
        image: z.string().optional(),
        url: z.string().optional(),
        categories: z.array(z.string()).optional(),
        type: z.string().optional(),
        datasets: z.array(z.object({
          id: z.string(),
          label: z.string(),
        })).optional(),
        tags: z.array(z.string()).optional(),
        rawbody: z.string(),
      })
    })
  }
})
