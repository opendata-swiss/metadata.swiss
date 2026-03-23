export const loadPageBreadcrumb = (locale: Ref<string>): LoadBreadcrumbContent => ({ path }) => {
  return queryCollection('pages')
    .select('id', 'title')
    .where('path', 'LIKE', `%${path}.${locale.value}`)
}

export const loadHandbookBreadcrumb = (locale: Ref<string>): LoadBreadcrumbContent => ({ path }, index) => {
  if (index === 0) {
    return loadPageBreadcrumb(locale)({ path }, index)
  }

  const segments = path.split('/').filter(Boolean)
  if (segments[0] === 'handbook') segments.shift()
  const slug = segments[segments.length - 1]

  return queryCollection('handbook')
    .select('id', 'title', 'breadcrumb_title', 'slug', 'parent', 'path')
    .where('path', 'LIKE', `%.${locale.value}`)
    .where('slug', '=', slug)
}
