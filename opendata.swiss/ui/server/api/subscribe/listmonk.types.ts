export interface Envelope<T> {
  data: T
}

interface Attribs extends Record<string, unknown> {
  datasets?: string[]
  categories?: string[]
  organisations?: string[]
}

export interface Subscriber {
  id: number
  name: string
  status: 'enabled' | 'blocklisted'
  lists?: number[]
  attribs?: Attribs
}

export interface Subscribers {
  results: Array<Subscriber>
}
