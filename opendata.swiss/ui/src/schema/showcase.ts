import { z } from 'zod/v4'
import { APP_LANGUAGES } from '../../app/constants/langages.js'

export const relationshipRole = z.enum([
  'author',
  'collaborator',
])

export const shape = {
  active: z.boolean(),
  pinned: z.boolean(),
  title: z.string().optional(),
  image: z.string().optional(),
  url: z.string().optional(),
  categories: z.array(z.string()).optional(),
  type: z.string(),
  datasets: z.array(z.object({
    id: z.string(),
    label: z.string(),
  })).optional(),
  relationships: z.array(
    z.discriminatedUnion('type', [
      z.object({
        type: z.literal('organization'),
        organization: z.array(z.object({
          id: z.string(),
          label: z.string(),
        })).nonempty(),
        role: relationshipRole,
      }),
      z.object({
        type: z.literal('organization-external'),
        name: z.string(),
        url: z.array(z.string()).optional(),
        role: relationshipRole,
      }),
      z.object({
        type: z.literal('person'),
        name: z.string(),
        role: relationshipRole,
      }),
    ]),
  ).optional(),
  tags: z.array(z.string()).optional(),
  rawbody: z.string().optional(),
}

const submissionSchemaShape = APP_LANGUAGES.reduce((acc, lang) => {
  acc[lang] = z.object(shape)
  return acc
}, {} as Record<string, z.ZodObject<typeof shape>>)

export const submissionSchema = (t: (key: string) => string) => z.object(submissionSchemaShape).refine(data =>
  APP_LANGUAGES.some((lang) => {
    const langData = data[lang]
    return langData && langData.title && langData.rawbody && langData.title.length >= 5 && langData.rawbody.length >= 100
  }),
{
  message: t('server.api.showcases.post.error.missing_content'),
  path: [],
})

export default z.object(shape)
