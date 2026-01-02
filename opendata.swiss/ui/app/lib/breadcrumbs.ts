export const loadPageBreadcrumb = (locale: Ref<string>): LoadBreadcrumbContent => ({ path }) => {
  return queryCollection('pages')
    .select('id', 'title')
    .where('path', 'LIKE', `%${path}.${locale.value}`)
}

export const loadHandbookSectionBreadcrumb = (section: string, locale: Ref<string>): LoadBreadcrumbContent => ({ path }, index) => {
  if (index === 0) {
    return loadPageBreadcrumb(locale)({ path }, index)
  }

  return queryCollection('handbookSections')
    .select('id', 'title')
    .where('title', '=', section)
}

export const loadHandbookBreadcrumb = (section: string, locale: Ref<string>): LoadBreadcrumbContent => ({ path }, index) => {
  if (index <= 1) {
    return loadHandbookSectionBreadcrumb(section, locale)({ path }, index)
  }

  return queryCollection('handbook')
    .select('id', 'title', 'breadcrumb_title')
    .where('path', 'LIKE', `%.${locale.value}`)
    .where('section', '=', section)
    .orWhere(q => q
      .where('permalink', '=', path.replace(/^\/handbook\/(?<section>\w+\/)/, ''))
      .where('path', '=', `${path}.${locale.value}`)
      .where('path', '=', `${path}/index.${locale.value}`),
    )
}
