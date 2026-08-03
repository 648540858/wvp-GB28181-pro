import { createSvgSymbol } from '@/icons/sprite'

describe('SVG sprite registration', () => {
  it('parses SVG files that contain XML and DOCTYPE declarations', () => {
    const source = '<?xml version="1.0"?><!DOCTYPE svg PUBLIC "-//W3C//DTD SVG 1.1//EN" "http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd"><svg viewBox="0 0 24 24" fill="currentColor"><path d="M1 1h22v22H1z" /></svg>'

    const symbol = createSvgSymbol(source, 'icon-example')

    expect(symbol.tagName.toLowerCase()).toBe('symbol')
    expect(symbol.getAttribute('id')).toBe('icon-example')
    expect(symbol.getAttribute('viewBox')).toBe('0 0 24 24')
    expect(symbol.getAttribute('fill')).toBe('currentColor')
    expect(symbol.querySelector('path')).not.toBeNull()
  })

  it('supports asset modules that expose SVG source through default', () => {
    const symbol = createSvgSymbol({
      default: '<svg viewBox="0 0 16 16"><circle cx="8" cy="8" r="8" /></svg>'
    }, 'icon-circle')

    expect(symbol.getAttribute('id')).toBe('icon-circle')
    expect(symbol.querySelector('circle')).not.toBeNull()
  })

  it('creates a viewBox for legacy SVG files that only declare dimensions', () => {
    const symbol = createSvgSymbol(
      '<svg width="130" height="130"><path d="M0 0h130v130H0z" /></svg>',
      'icon-legacy'
    )

    expect(symbol.getAttribute('viewBox')).toBe('0 0 130 130')
  })
})
