/**
 * 地图样式地址重写：
 * 样式 JSON 里 sources / sprite / glyphs 的地址可能和样式本身的地址不一致
 * （例如样式通过 https://host:3003 访问，source 却写着 http://another-host:3000），
 * 导致跨域、混合内容或瓦片加载失败。
 *
 * 这里在拿到样式 JSON 后，把这些地址统一改成「当前请求样式地址」的
 * 协议 + IP + 端口，路径、查询参数和 {z}/{x}/{y} 模板保持原样。
 */

function getStyleBase(styleUrl) {
  const base = (typeof window !== 'undefined' && window.location)
    ? window.location.href
    : 'http://localhost/'
  return new URL(styleUrl, base)
}

function splitSuffix(raw) {
  let rest = raw
  let hash = ''
  const hashIndex = rest.indexOf('#')
  if (hashIndex >= 0) {
    hash = rest.slice(hashIndex)
    rest = rest.slice(0, hashIndex)
  }
  let query = ''
  const queryIndex = rest.indexOf('?')
  if (queryIndex >= 0) {
    query = rest.slice(queryIndex)
    rest = rest.slice(0, queryIndex)
  }
  return { path: rest, query: query, hash: hash }
}

function normalizePath(path) {
  const parts = path.split('/')
  const stack = []
  for (let i = 0; i < parts.length; i++) {
    const part = parts[i]
    if (i === 0 && part === '') {
      stack.push('')
      continue
    }
    if (part === '' || part === '.') {
      continue
    }
    if (part === '..') {
      if (stack.length > 1) {
        stack.pop()
      }
      continue
    }
    stack.push(part)
  }
  if (stack.length === 0 || stack[0] !== '') {
    stack.unshift('')
  }
  return stack.join('/') || '/'
}

// 绝对地址 / 协议相对地址：只替换 scheme://host:port，path 和 query 原样保留。
// 不能用 new URL(...).href 重新序列化，否则 {z}/{x}/{y} 会被编码成 %7Bz%7D。
function rewriteAbsolute(raw, origin) {
  if (/^[a-z][a-z0-9+.-]*:\/\//i.test(raw)) {
    return raw.replace(/^[a-z][a-z0-9+.-]*:\/\/[^/]*/i, origin)
  }
  return raw.replace(/^\/\/[^/]*/, origin)
}

function rewriteRelative(raw, styleUrl, origin) {
  const suffix = splitSuffix(raw)
  let path = suffix.path
  if (path.charAt(0) !== '/') {
    const base = getStyleBase(styleUrl)
    const dir = base.pathname.slice(0, base.pathname.lastIndexOf('/') + 1)
    path = dir + path
  }
  return origin + normalizePath(path) + suffix.query + suffix.hash
}

export function rewriteUrlOrigin(raw, styleUrl, origin) {
  if (typeof raw !== 'string' || raw === '' || typeof styleUrl !== 'string' || styleUrl === '') {
    return raw
  }
  // data:/blob:/pmtiles: 等非 http(s) 地址保持原样
  if (/^[a-z][a-z0-9+.-]*:/i.test(raw)) {
    if (!/^https?:/i.test(raw)) {
      return raw
    }
    return rewriteAbsolute(raw, origin)
  }
  if (raw.slice(0, 2) === '//') {
    return rewriteAbsolute(raw, origin)
  }
  if (raw.charAt(0) === '#') {
    return raw
  }
  return rewriteRelative(raw, styleUrl, origin)
}

function rewriteSprite(sprite, styleUrl, origin) {
  if (typeof sprite === 'string') {
    return rewriteUrlOrigin(sprite, styleUrl, origin)
  }
  if (Array.isArray(sprite)) {
    return sprite.map((item) => {
      if (typeof item === 'string') {
        return rewriteUrlOrigin(item, styleUrl, origin)
      }
      if (item && typeof item === 'object' && typeof item.url === 'string') {
        return Object.assign({}, item, {
          url: rewriteUrlOrigin(item.url, styleUrl, origin)
        })
      }
      return item
    })
  }
  return sprite
}

export function rewriteStyleOrigin(style, styleUrl) {
  if (!style || typeof style !== 'object' || typeof styleUrl !== 'string' || styleUrl === '') {
    return style
  }
  const origin = getStyleBase(styleUrl).origin
  // 不改动传入对象，避免影响调用方缓存
  const next = JSON.parse(JSON.stringify(style))

  if (next.sources && typeof next.sources === 'object') {
    Object.keys(next.sources).forEach((key) => {
      const source = next.sources[key]
      if (!source || typeof source !== 'object') {
        return
      }
      if (typeof source.url === 'string') {
        source.url = rewriteUrlOrigin(source.url, styleUrl, origin)
      }
      if (Array.isArray(source.tiles)) {
        source.tiles = source.tiles.map((tileUrl) => rewriteUrlOrigin(tileUrl, styleUrl, origin))
      }
      if (typeof source.data === 'string') {
        source.data = rewriteUrlOrigin(source.data, styleUrl, origin)
      }
    })
  }

  if (next.sprite !== undefined) {
    next.sprite = rewriteSprite(next.sprite, styleUrl, origin)
  }
  if (typeof next.glyphs === 'string') {
    next.glyphs = rewriteUrlOrigin(next.glyphs, styleUrl, origin)
  }

  return next
}
