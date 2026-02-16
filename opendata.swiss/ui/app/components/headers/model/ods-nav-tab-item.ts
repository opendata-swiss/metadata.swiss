interface OdsNavTabLinkItem {
  label: string
  to?: string
}

export interface OdsNavTabItem extends OdsNavTabLinkItem {
  subMenu?: OdsNavTabLinkItem[]
  adminOnly?: boolean
}
