import type { PagesCollectionItem } from '@nuxt/content'

export async function useNavigationItems() {
  const { t, locale } = useI18n()

  const { data: pages } = await useAsyncData('navigation-pages', () => {
    return queryCollection('pages')
      .where('path', 'LIKE', `%.${locale.value}`)
      .where('mainMenu', '=', true)
      .all()
  })

  const stemPattern = /pages\/(?<name>.+)\.\w\w/i
  const getSlug = (page: PagesCollectionItem) => page.stem.match(stemPattern)?.groups?.name

  const sortedPages = pages.value?.slice() || []
  const maxIterations = sortedPages.length
  for (let iter = 0; iter < maxIterations; iter++) {
    let changed = false
    for (let i = 0; i < sortedPages.length; i++) {
      const a = sortedPages[i]
      if (a?.after) {
        const targetIndex = sortedPages.findIndex(p => getSlug(p) === a.after)
        if (targetIndex !== -1 && targetIndex > i) {
          const aToMove = sortedPages.splice(i, 1)[0]!
          // Find target again because index shifted if i < targetIndex
          const newTargetIndex = sortedPages.findIndex(p => getSlug(p) === a.after)
          sortedPages.splice(newTargetIndex + 1, 0, aToMove)
          changed = true
          break
        }
      }
    }
    if (!changed) break
  }

  const pagesSubMenu = sortedPages.map((page) => {
    const path = page.permalink ? page.permalink : getSlug(page)
    return ({
      label: page.title,
      to: `/${path}`,
    })
  })

  return [
    {
      label: t('message.header.navigation.home'),
      to: '/',
    },
    {
      label: t('message.header.navigation.datasets'),
      to: '/datasets',
    },
    {
      label: t('message.header.navigation.showcases'),
      to: '/showcases',
    },
    {
      label: t('message.header.navigation.blog'),
      to: '/blog',
    },
    {
      label: t('message.header.navigation.handbook'),
      to: '/handbook',
    },
    {
      label: t('message.header.navigation.admin.title'),
      adminOnly: true,
      subMenu: [
        { label: t('message.header.navigation.admin.dashboard'), to: '/gogd/dashboard' },
        { label: t('message.header.navigation.admin.metrics'), to: '/gogd/metrics' },
        { label: t('message.header.navigation.admin.categories'), to: '/gogd/categories' },
      ],
    },
    {
      label: t('message.header.navigation.more'),
      subMenu: pagesSubMenu,
    },
  ]
}
