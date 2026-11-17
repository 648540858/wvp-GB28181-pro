/**
 * Created by PanJiaChen on 16/11/18.
 */

/**
 * @param {string} path
 * @returns {Boolean}
 */
export function isExternal(path) {
  return /^(https?:|mailto:|tel:)/.test(path)
}

/**
 * @param {string} str
 * @returns {Boolean}
 */
export function validUsername(str) {
  return typeof str === 'string' && str.length > 0
}

/**
 * 解析路由路径。替代 Node 的 path.resolve：
 * webpack 5 不再自动注入 path 等 Node 核心模块的 polyfill。
 * @param {string} basePath
 * @param {string} routePath
 * @returns {string}
 */
export function resolvePath(basePath, routePath) {
  if (routePath && routePath.charAt(0) === '/') {
    return routePath
  }
  const base = basePath || '/'
  const joined = (base.charAt(base.length - 1) === '/' ? base : base + '/') + (routePath || '')
  const stack = []
  joined.split('/').forEach((segment) => {
    if (segment === '' || segment === '.') {
      return
    }
    if (segment === '..') {
      stack.pop()
      return
    }
    stack.push(segment)
  })
  return '/' + stack.join('/')
}
