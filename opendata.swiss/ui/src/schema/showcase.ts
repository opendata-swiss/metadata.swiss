import { z } from 'zod/v4'
import { APP_LANGUAGES } from '../../app/constants/langages.js'

export const shape = {
  active: z.boolean(),
  title: z.string().optional(),
  body: z.string().optional(),
  image: z.string().optional(),
  url: z.string().optional(),
  categories: z.array(z.string()).optional(),
  type: z.string(),
  datasets: z.array(z.object({
    id: z.string(),
    label: z.string(),
  })).optional(),
  tags: z.array(z.string()).optional(),
  submittedBy: z.object({
    name: z.string(),
    url: z.array(z.url()).optional(),
  }).optional(),
  rawbody: z.string().optional(),
}

const submissionSchemaShape = APP_LANGUAGES.reduce((acc, lang) => {
  acc[lang] = z.object(shape)
  return acc
}, {} as Record<string, z.ZodObject<typeof shape>>)

export const submissionSchema = (t: (key: string) => string) => z.object(submissionSchemaShape).refine(data =>
  APP_LANGUAGES.some((lang) => {
    const langData = data[lang]
    return langData.title && langData.body && langData.title.length >= 5 && langData.body.length >= 100
  }),
{
  message: t('server.api.showcases.post.error.missing_content'),
  path: [],
})

export default z.object(shape)
