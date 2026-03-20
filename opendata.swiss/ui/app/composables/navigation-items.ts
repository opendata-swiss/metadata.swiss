export async function useNavigationItems() {
  const { t, locale } = useI18n()

  const { data: pages } = await useAsyncData('navigation-pages', () => {
    return queryCollection('pages')
      .where('path', 'LIKE', `%.${locale.value}`)
      .where('mainMenu', '=', true)
      .all()
  })

  const stemPattern = /pages\/(?<name>.+)\.\w\w/i
  const pagesSubMenu = pages.value!.map((page) => {
    const path = page.permalink ? page.permalink : page.stem.match(stemPattern)?.groups?.name
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
