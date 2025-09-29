import type { OdsNavTabItem } from '../components/headers/model/ods-nav-tab-item';

export const APP_NAVIGATION_ITEMS: OdsNavTabItem[] = [
  {
    label: 'message.header.navigation.home',
    to: '/'
  },
  {
    label: 'message.header.navigation.datasets',
    to: '/datasets'
  },
  {
    label: 'message.header.navigation.showcases',
    to: '/showcases'
  },
  {
    label: 'message.header.navigation.blog',
    to: '/blog'
  },
  {
    label: 'message.header.navigation.handbook',
    to: '/handbook'
  },
  {
    label: 'message.header.navigation.about',
    subMenu: [
      { label: 'message.header.navigation.what_is_ods', to: '/about' },
      { label: 'message.header.navigation.terms_of_use', to: '/terms-of-use' },
      { label: 'message.header.navigation.contact', to: '/contact' },
    ]
  }
]
