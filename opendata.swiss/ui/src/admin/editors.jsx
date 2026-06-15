import CMS from 'decap-cms-app'

CMS.registerEditorComponent({
  id: 'ods-section',
  label: 'Section',
  fields: [{
    name: 'title',
    label: 'Title',
    widget: 'string',
  }, {
    name: 'content',
    label: 'Content',
    widget: 'markdown',
  }],
  collapsed: true,
  pattern: /^\w*::OdsSection(?:\{title="?(?<title>[^"]*)"?\})?\n(?<content>.*?)\n::/ms,
  fromBlock: (match) => {
    return {
      title: match?.groups?.title || '',
      content: match?.groups?.content.replace(/^ {4}/gm, '') || '',
    }
  },
  toBlock: (obj) => {
    const props = []
    if (obj.title) {
      props.push(['title', obj.title])
    }

    const propsString = !props.length
      ? ''
      : `{${props.map(([name, value]) => `${name}="${value}"`).join(' ')}}`

    return `::OdsSection${propsString}
${obj.content.replaceAll(/^/gm, '    ')}
::`
  },
})

CMS.registerEditorComponent({
  id: 'ods-card',
  label: 'Card',
  collapsed: true,
  fields: [{
    name: 'title',
    label: 'Title',
    widget: 'string',
  }, {
    name: 'content',
    label: 'Content',
    widget: 'markdown',
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
  pattern: /^\w*::OdsCard(?:\{title="?(?<title>[^"]*)"?\})?\n(?<content>.*?)\n::/ms,
  fromBlock: (match) => {
    const content = match?.groups?.content || ''
    const image = content.match(/^#image.*!\[]\(([^#)]+)\)*/sm)?.[1] || ''
    const href = content.match(/^#footer-action[\t ]*\r?\n[\s\S]*?::OdsButton\{[^}\n]*\bhref="([^"\n]+)"/m)?.[1] || ''
    const metaBlock = content.match(/^#top-meta\b[\s\S]*?(?=^#\w|(?![\s\S]))/sm)?.[0] || ''
    const meta = [...metaBlock.matchAll(/^\s*::OdsMetaInfo\{[^}]*\btext="([^"]*)"[^}]*}\s*\r?\n\s*::\s*$/gm)]
      .map(m => m[1].trim())
      .filter(Boolean)

    return {
      title: match?.groups?.title || '',
      content: content.match(/^([^#]+)#/s)?.[1],
      image,
      href,
      meta,
    }
  },
  toBlock: (obj) => {
    const props = []
    if (obj.title) {
      props.push(['title', obj.title])
    }

    const propsString = !props.length
      ? ''
      : `{${props.map(([name, value]) => `${name}="${value}"`).join(' ')}}`

    const blockLines = [
      `${obj.content || ''}`.trim(),
    ]

    if (obj.image) {
      blockLines.push(`\n#image\n![](${obj.image})`)
    }

    if (obj.href) {
      blockLines.push(`
#footer-action
    ::OdsButton{icon=ArrowRight variant=outline :iconOnly="true" href="${obj.href}"}
    ::`)
    }

    if (obj.meta?.length) {
      blockLines.push(`
#top-meta
${obj.meta.map(meta => `    ::OdsMetaInfo{text="${meta}"}
    ::`).join('\n')}`)
    }

    return [
      `::OdsCard${propsString}`,
      ...blockLines,
      '::',
    ].join('\n')
  },
})
