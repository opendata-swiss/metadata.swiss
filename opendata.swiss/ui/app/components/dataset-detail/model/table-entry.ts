export interface TableEntry {
  label: string
  value: [
    {
      value: string
      href?: string
      type: 'value' | 'href' | 'email' | 'telephone'
    },
  ]
  nodeType: 'node' | 'value' | 'href'
}
