import {z} from "zod/v4";

export const shape = {
  active: z.boolean(),
  title: z.string().min(5),
  image: z.string().optional(),
  url: z.string().optional(),
  categories: z.array(z.string()).optional(),
  type: z.string(),
  datasets: z.array(z.object({
    id: z.string(),
    label: z.string(),
  })).optional(),
  tags: z.array(z.string()).optional(),
  rawbody: z.string().optional(),
}

export default z.object(shape)
