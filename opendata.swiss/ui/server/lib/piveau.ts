import type { AppLanguage } from '~/constants/langages'

interface Category {
  id: string
}

interface ContactPoint {
  name: string
  email: string
}

export interface Dataset {
  id: string
  categories?: Category[]
  title: Record<AppLanguage, string>
  contact_point?: ContactPoint[]
}

interface SearchResult {
  result: {
    results: Dataset[]
  }
}

interface SearchArgs {
  sort?: string
  minDate?: Date
  limit?: number
  dateType: 'issue' | 'modified' | 'temporal'
}

export class HubSearch {
  constructor(private baseUrl: string) {
  }

  get datasets() {
    return {
      search: async ({ sort, minDate, limit }: SearchArgs) => {
        const searchUrl = new URL('search', this.baseUrl)

        searchUrl.searchParams.set('filters', 'dataset')
        if (sort) searchUrl.searchParams.set('sort', sort)
        if (limit) searchUrl.searchParams.set('limit', limit.toString())
        if (minDate) searchUrl.searchParams.set('minDate', minDate.toISOString())

        const searchRes = await fetch(searchUrl)

        if (!searchRes.ok) {
          return new Error(`Failed to fetch datasets: ${searchRes.status} ${searchRes.statusText}`, {
            cause: await searchRes.text(),
          })
        }

        const searchResult: SearchResult = await searchRes.json()

        return searchResult.result.results
      },

      get: async (id: string): Promise<Dataset | Error> => {
        const url = new URL(`datasets/${id}`, this.baseUrl)

        const res = await fetch(url)

        if (!res.ok) {
          return new Error(`Failed to fetch dataset ${id}: ${res.status} ${res.statusText}`, {
            cause: await res.text(),
          })
        }

        const json = await res.json()
        return json.result
      },
    }
  }
}
