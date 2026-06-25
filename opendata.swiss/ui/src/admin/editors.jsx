import CMS from 'decap-cms-app'

function registerEditorComponent({ id, label, fields = [], content }) {
  const propsPattern = /:?(?<name>\w+)=(?:"(?<value>[^"]*)"|'(?<jsonValue>[^']*)')/g
  // Match outer block `::id ... ::` while allowing nested `::Name ... ::` blocks inside `content`.
  // Strategy: In `content`, consume either a full nested block starting with `::Word` up to its own closing `::`,
  // or any other character that is not the section-closing line `::` by itself.
  // Match a `{...}` fields block while allowing `}` to appear inside quoted values (e.g. JSON props).
  const fieldsPattern = String.raw`\{(?:[^{}'"]|'[^']*'|"[^"]*")+\}`
  const nestedBlockPattern = String.raw`::\w+(?:${fieldsPattern})?(?:\n[\s\S]*?\n::)`
  // Use greedy repetition so the outer closer matches the last `::` line, not the first nested one.
  const contentPattern = String.raw`(?:(?:${nestedBlockPattern})|(?:(?!^::\s*$)[\s\S]))*`
  const pattern = new RegExp(`^::${id}(?<fields>${fieldsPattern})?(?:\\n(?<content>${contentPattern}))?\\n::$`, 'ms')

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
        const { name, value, jsonValue } = matchResult.groups
        if (jsonValue !== undefined) {
          try {
            props[name] = JSON.parse(jsonValue)
          }
          catch {
            props[name] = jsonValue
          }
        }
        else {
          props[name] = value
        }
      }

      if (content) {
        props.content = match?.groups?.content || ''
      }

      return props
    },
    toBlock: (props) => {
      let propsString = Object.entries(props)
        .filter(([name]) => name !== 'content')
        .map(([name, value]) => {
          const isJson = fields
            .find(field => field.name === name)
            ?.json === true

          if (isJson) {
            return `:${name}='${JSON.stringify(value)}'`
          }

          return `${name}="${value}"`
        })
        .join(' ')
      if (propsString.length) {
        propsString = `{${propsString}}`
      }
      return `::${id}${propsString}${props.content ? `\n${props.content}` : ''}\n::`
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

registerEditorComponent({
  id: 'OdsSectionPromotedShowcases',
  label: 'Promoted Showcases',
  fields: [{
    name: 'title',
    label: 'Title',
    widget: 'string',
  }, {
    name: 'max',
    label: 'Max',
    widget: 'number',
    min: 1,
    max: 5,
    default: 5,
    json: true,
  }, {
    name: 'showcases',
    label: 'Showcases',
    hint: 'Manually select showcase to promote. More will be loaded dynamically to fill the section to the max number.',
    widget: 'piveau-showcase',
    piveau: {
      search: import.meta.env.NUXT_PUBLIC_PIVEAU_HUB_SEARCH_URL || 'https://piveau-hub-search.int.ods.zazukoians.org/',
    },
    json: true,
  }],
})
