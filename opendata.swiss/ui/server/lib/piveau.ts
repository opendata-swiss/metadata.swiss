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

interface SearchResult<T> {
  result: {
    scrollId?: string
    results: T[]
  }
}

interface SearchArgs {
  sort?: string
  minDate?: Date
  limit?: number
  dateType?: 'issue' | 'modified' | 'temporal'
}

export class HubSearch {
  constructor(private baseUrl: string, private _fetch = fetch) {
  }

  get datasets() {
    return {
      search: ({ sort, minDate, limit, dateType }: SearchArgs = {}) => {
        const searchUrl = new URL('search', this.baseUrl)

        searchUrl.searchParams.set('filters', 'dataset')
        if (sort) searchUrl.searchParams.set('sort', sort)
        if (limit) searchUrl.searchParams.set('limit', limit.toString())
        if (minDate) searchUrl.searchParams.set('minDate', minDate.toISOString())
        if (dateType) searchUrl.searchParams.set('dateType', dateType)

        return {
          all: async () => {
            const searchRes = await this._fetch(searchUrl)

            if (!searchRes.ok) {
              return new Error(`Failed to fetch datasets: ${searchRes.status} ${searchRes.statusText}`, {
                cause: await searchRes.text(),
              })
            }
            const searchResult: SearchResult<Dataset> = await searchRes.json()

            return searchResult.result.results
          },
          scroll: this.searchScroll<Dataset>(searchUrl),
        }
      },

      get: async (id: string): Promise<Dataset | Error> => {
        const url = new URL(`datasets/${id}`, this.baseUrl)

        const res = await this._fetch(url)

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

  private searchScroll<T>(searchUrl: URL) {
    const scrollUrl = new URL('scroll', this.baseUrl)
    const scrollInitUrl = new URL(searchUrl)
    scrollInitUrl.searchParams.set('scroll', 'true')

    return async function* (this: HubSearch) {
      const searchRes: SearchResult<Dataset> = await (await this._fetch(scrollInitUrl)).json()
      if (!searchRes.result.scrollId) {
        return new Error('Scroll ID missing in response')
      }
      scrollUrl.searchParams.set('scrollId', searchRes.result.scrollId)

      yield searchRes.result.results

      let scrollRes
      do {
        scrollRes = await this._fetch(scrollUrl)
        if (scrollRes.ok) {
          const searchResult: SearchResult<T> = await scrollRes.json()
          yield searchResult.result.results
        }
      } while (scrollRes.ok)
    }.bind(this)
  }
}
