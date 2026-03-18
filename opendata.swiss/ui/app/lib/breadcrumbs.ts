export const loadPageBreadcrumb = (locale: Ref<string>): LoadBreadcrumbContent => ({ path }) => {
  return queryCollection('pages')
    .select('id', 'title')
    .where('path', 'LIKE', `%${path}.${locale.value}`)
}

export const loadHandbookBreadcrumb = (locale: Ref<string>): LoadBreadcrumbContent => ({ path }, index) => {
  if (index === 0) {
    return loadPageBreadcrumb(locale)({ path }, index)
  }

  const slug = path.split('/').pop()

  return queryCollection('handbook')
    .select('id', 'title', 'breadcrumb_title')
    .where('path', 'LIKE', `%.${locale.value}`)
    .orWhere(q => q
      .where('slug', '=', slug)
      .where('path', '=', `${path}.${locale.value}`)
      .where('path', '=', `${path}/index.${locale.value}`),
    )
}
