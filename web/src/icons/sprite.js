const svgNamespace = 'http://www.w3.org/2000/svg'
const ignoredAttributes = ['height', 'id', 'version', 'width', 'xmlns', 'xmlns:xlink']

export function createSvgSymbol(source, id) {
  const content = source && source.default ? source.default : source
  if (typeof content !== 'string') {
    return null
  }

  const parsed = new DOMParser().parseFromString(content, 'image/svg+xml')
  const svg = parsed.documentElement
  if (!svg || svg.nodeName.toLowerCase() !== 'svg' || parsed.querySelector('parsererror')) {
    return null
  }

  const symbol = document.createElementNS(svgNamespace, 'symbol')
  symbol.setAttribute('id', id)
  const width = parseFloat(svg.getAttribute('width'))
  const height = parseFloat(svg.getAttribute('height'))
  if (!svg.hasAttribute('viewBox') && width > 0 && height > 0) {
    symbol.setAttribute('viewBox', `0 0 ${width} ${height}`)
  }
  Array.from(svg.attributes).forEach(attribute => {
    if (ignoredAttributes.indexOf(attribute.name) < 0) {
      symbol.setAttribute(attribute.name, attribute.value)
    }
  })
  Array.from(svg.childNodes).forEach(child => {
    symbol.appendChild(document.importNode(child, true))
  })
  return symbol
}

export function registerSvgSprite(modules) {
  const existingSprite = document.getElementById('wvp-svg-sprite')
  if (existingSprite) {
    return existingSprite
  }

  const sprite = document.createElementNS(svgNamespace, 'svg')
  sprite.setAttribute('id', 'wvp-svg-sprite')
  sprite.setAttribute('aria-hidden', 'true')
  sprite.style.position = 'absolute'
  sprite.style.width = '0'
  sprite.style.height = '0'

  Object.entries(modules).forEach(([key, source]) => {
    const id = `icon-${key.replace(/^.*\//, '').replace(/\.svg$/, '')}`
    const symbol = createSvgSymbol(source, id)
    if (symbol) {
      sprite.appendChild(symbol)
    }
  })
  document.body.insertBefore(sprite, document.body.firstChild)
  return sprite
}
