import { z } from 'zod/v4'
import { APP_LANGUAGES } from '../../app/constants/langages.js'

export const shape = {
  active: z.boolean(),
  pinned: z.boolean(),
  title: z.string().min(5),
  images: z.array(z.string()).min(1),
  url: z.string(),
  themes: z.array(z.string()).optional(),
  type: z.string(),
  createdBy: z.string(),
  datasets: z.array(z.object({
    id: z.string(),
    label: z.string(),
  })).min(1),
  keywords: z.array(z.string()).optional(),
  contactDetails: z.object({
    name: z.string().nonempty(),
    email: z.string().nonempty(),
    github: z.string().optional(),
  }),
  more: z.object({
    whoAreYou: z.string().optional(),
    goal: z.string().optional(),
    why: z.string().optional(),
    users: z.string().optional(),
    challengesBefore: z.string().optional(),
    challengesCreating: z.string().optional(),
    getMost: z.string().optional(),
    anythingElse: z.string().optional(),
  }).optional(),
  rawbody: z.string().optional(),
}

const submissionSchemaShape = APP_LANGUAGES.reduce((acc, lang) => {
  acc[lang] = z.object(shape)
  return acc
}, { slug: z.string().min(5) } as Record<string, z.ZodObject<typeof shape>> & { slug: z.ZodString })

export const submissionSchema = z.object(submissionSchemaShape)

export default z.object(shape)
