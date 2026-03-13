import type { OdsNavTabItem } from '../components/headers/model/ods-nav-tab-item'

export function useNavigationItemLabel() {
  const { t } = useI18n()
  return (item: OdsNavTabItem) => item.label.startsWith('message.') ? t(item.label) : item.label
}

export const useNavigationItems = async (): Promise<OdsNavTabItem[]> => {
  const { locale } = useI18n()

  const { data } = await useAsyncData('navigation items', () => {
    return queryCollection('handbookSections')
      .where('path', 'LIKE', `%.${locale.value}`)
      .order('order', 'ASC')
      .all()
  })

  return [
    {
      label: 'message.header.navigation.home',
      to: '/',
    },
    {
      label: 'message.header.navigation.datasets',
      to: '/datasets',
    },
    {
      label: 'message.header.navigation.showcases',
      to: '/showcases',
    },
    {
      label: 'message.header.navigation.blog',
      to: '/blog',
    },
    {
      label: 'message.header.navigation.handbook',
      subMenu: [
        { label: 'message.header.navigation.handbook', to: '/handbook' },
        ...data.value?.map(section => ({ label: section.title, to: `/handbook/${section.title.toLowerCase()}` })) || [],
      ],
    },
    {
      label: 'message.header.navigation.about',
      subMenu: [
        { label: 'message.header.navigation.what_is_ods', to: '/about' },
        { label: 'message.header.navigation.terms_of_use', to: '/terms-of-use' },
        { label: 'message.header.navigation.contact', to: '/contact' },
      ],
    },
    {
      label: 'message.header.navigation.admin.title',
      adminOnly: true,
      subMenu: [
        { label: 'message.header.navigation.admin.dashboard', to: '/gogd/dashboard' },
        { label: 'message.header.navigation.admin.metrics', to: '/gogd/metrics' },
        { label: 'message.header.navigation.admin.categories', to: '/gogd/categories' },
      ],
    },
  ]
}
