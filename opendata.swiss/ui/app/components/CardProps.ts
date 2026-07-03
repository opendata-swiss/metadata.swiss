export default interface CardProps {
  title: string
  type?: 'default' | 'highlight' | 'twitter' | 'flat' | 'universal' | 'list'
  clickable?: boolean
  href?: string
}
