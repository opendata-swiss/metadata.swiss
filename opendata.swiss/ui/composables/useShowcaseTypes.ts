import { useVocabularySearch } from '~/piveau/vocabularies'

/**
 * Shared cache for showcase types so the query runs only once per SSR request
 * and once on the client. Subsequent consumers reuse the same data.
 */
export function useShowcaseTypes() {
  type ShowcaseType = { resource: string, pref_label?: string }

  const data = useState<ShowcaseType[]>('showcase-types:data', () => [])
  const loaded = useState<boolean>('showcase-types:loaded', () => false)
  const pending = useState<boolean>('showcase-types:pending', () => false)
  const error = useState<unknown | null>('showcase-types:error', () => null)

  // Set up the vocabulary search once; it's cheap to create, the fetch is gated below.
  const { query, getSearchResultsEnhanced } = useVocabularySearch().useSearch({
    queryParams: { vocabulary: 'showcase-types' },
  })

  async function ensureLoaded() {
    if (loaded.value || pending.value) return data
    pending.value = true
    try {
      await query.suspense()
      data.value = getSearchResultsEnhanced.value ?? []
      loaded.value = true
    }
    catch (e) {
      error.value = e
      // Do not mark loaded so another attempt may retry later
    }
    finally {
      pending.value = false
    }
    return data
  }

  return { data, loaded, pending, error, ensureLoaded }
}
