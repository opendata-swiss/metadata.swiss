import CMS from 'decap-cms-app'

function registerEditorComponent({ id, label, fields = [], content }) {
  const propsPattern = /(?<name>\w+)="(?<value>[^"]+)"/g
  // Match outer block `::id ... ::` while allowing nested `::Name ... ::` blocks inside `content`.
  // Strategy: In `content`, consume either a full nested block starting with `::Word` up to its own closing `::`,
  // or any other character that is not the section-closing line `::` by itself.
  const nestedBlockPattern = String.raw`::\w+(?:\{[^}]+})?(?:\n[\s\S]*?\n::)`
  // Use greedy repetition so the outer closer matches the last `::` line, not the first nested one.
  const contentPattern = String.raw`(?:(?:${nestedBlockPattern})|(?:(?!^::\s*$)[\s\S]))*`
  const pattern = new RegExp(`^::${id}(?<fields>\\{[^}]+})?(?:\\n(?<content>${contentPattern}))?\\n::$`, 'ms')

  if (content) {
    fields.push({
      name: 'content',
      label: 'Content',
      widget: 'markdown',
    })
  }

  CMS.registerEditorComponent({
    id,
    label,
    fields,
    collapsed: true,
    pattern,
    fromBlock: (match) => {
      const props = {}
      let matchResult
      while ((matchResult = propsPattern.exec(match?.groups?.fields)) !== null) {
        props[matchResult.groups.name] = matchResult.groups.value
      }

      if (content) {
        props.content = match?.groups?.content || ''
      }

      return props
    },
    toBlock: (props) => {
      let propsString = Object.entries(props)
        .filter(([name]) => name !== 'content')
        .map(([name, value]) => `${name}="${value}"`)
        .join(' ')
      if (propsString.length) {
        propsString = `{${propsString}}`
      }
      return `::${id}${propsString}\n${props.content}\n::`
    },
  })
}

registerEditorComponent({
  id: 'OdsHeroSearch',
  label: 'Hero Search',
})

registerEditorComponent({
  id: 'OdsSection',
  label: 'Section',
  fields: [{
    name: 'title',
    label: 'Title',
    widget: 'string',
  }, {
    name: 'layout',
    label: 'Layout',
    widget: 'select',
    options: [{
      label: 'One Card',
      value: 'grid--items-1',
    }, {
      label: 'Two Cards',
      value: 'grid--items-2',
    }, {
      label: 'Three Cards',
      value: 'grid--items-3',
    }, {
      label: 'Four Cards',
      value: 'grid--items-4',
    }, {
      label: 'Five Cards',
      value: 'grid--items-5',
    }],
  }, {
    name: 'accentColor',
    label: 'Accent Color',
    widget: 'select',
    options: ['50', '100', '200', '300', '400', '500', '600', '700', '800', '900'],
  }, {
    name: 'textColor',
    label: 'Text Color',
    widget: 'select',
    options: ['50', '100', '200', '300', '400', '500', '600', '700', '800', '900'],
  }],
  collapsed: true,
  content: true,
})

registerEditorComponent({
  id: 'OdsCard',
  label: 'Card',
  collapsed: true,
  fields: [{
    name: 'type',
    label: 'Variant',
    widget: 'select',
    options: ['default', 'highlight', 'twitter', 'flat', 'universal', 'list'],
  }, {
    name: 'title',
    label: 'Title',
    widget: 'string',
  }, {
    name: 'image',
    label: 'Image',
    widget: 'image',
  }, {
    name: 'href',
    label: 'Link',
    widget: 'string',
  }, {
    name: 'meta',
    label: 'Meta Info',
    widget: 'list',
  }],
  content: true,
})
