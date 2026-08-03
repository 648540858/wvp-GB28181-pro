import path from 'path'
import { existsSync } from 'fs'
import { h } from 'vue'
import { createMemoryHistory, createRouter } from 'vue-router'
import { constantRoutes } from '@/router'

function resolveRoutePath(basePath, routePath) {
  if (routePath.startsWith('/')) return routePath
  return path.posix.resolve(basePath || '/', routePath || '')
}

function visibleMenuPaths(routes, basePath = '/') {
  return routes.flatMap(route => {
    if (route.hidden) return []

    const currentPath = resolveRoutePath(basePath, route.path)
    const visibleChildren = (route.children || []).filter(child => !child.hidden)
    if (route.onlyIndex >= 0 && route.children?.[route.onlyIndex]) {
      return visibleMenuPaths([route.children[route.onlyIndex]], currentPath)
    }
    if (visibleChildren.length === 1 && !route.alwaysShow) {
      return visibleMenuPaths(visibleChildren, currentPath)
    }
    if (visibleChildren.length > 0) {
      return visibleMenuPaths(visibleChildren, currentPath)
    }
    return [currentPath]
  })
}

const menuPaths = visibleMenuPaths(constantRoutes)

function flattenRoutes(routes, basePath = '/') {
  return routes.flatMap(route => {
    const fullPath = resolveRoutePath(basePath, route.path)
    return [{ route, fullPath }, ...flattenRoutes(route.children || [], fullPath)]
  })
}

function navigationRoutes(routes) {
  return routes.map(route => ({
    ...route,
    component: route.component ? { render: () => h('div') } : undefined,
    children: route.children ? navigationRoutes(route.children) : undefined
  }))
}

describe('application routes', () => {
  it('does not redirect a route back to itself', () => {
    const selfRedirects = constantRoutes
      .filter(route => typeof route.redirect === 'string' && route.redirect === route.path)
      .map(route => route.path)

    expect(selfRedirects).toEqual([])
  })

  it('keeps route names, redirects and menu metadata valid', () => {
    const routes = flattenRoutes(constantRoutes)
    const names = routes.map(({ route }) => route.name).filter(Boolean)
    const duplicateNames = names.filter((name, index) => names.indexOf(name) !== index)
    const router = createRouter({ history: createMemoryHistory(), routes: navigationRoutes(constantRoutes) })
    const invalidRedirects = routes
      .filter(({ route }) => typeof route.redirect === 'string')
      .filter(({ route }) => router.resolve(route.redirect).matched.length === 0)

    expect(duplicateNames).toEqual([])
    expect(invalidRedirects).toEqual([])
    menuPaths.forEach(menuPath => {
      const matched = router.resolve(menuPath).matched.at(-1)
      expect(matched?.meta?.title).toBeTruthy()
    })
    routes.forEach(({ route }) => {
      const icon = route.meta?.icon
      if (icon && !icon.startsWith('el-icon')) {
        expect(existsSync(path.resolve(__dirname, `../../../src/icons/svg/${icon}.svg`))).toBe(true)
      }
    })
  })

  it.each(menuPaths)('navigates to visible menu path %s', async menuPath => {
    const router = createRouter({ history: createMemoryHistory(), routes: navigationRoutes(constantRoutes) })

    await router.push(menuPath)

    expect(router.currentRoute.value.path).toBe(menuPath)
    expect(router.currentRoute.value.matched.at(-1)?.components?.default).toBeDefined()
  })

  it.each([
    ['/jtDevice', '/device/jtDevice'],
    ['/push', '/device/push'],
    ['/proxy', '/device/proxy'],
    ['/device/record/device-1/channel-1', '/device'],
    ['/jtDevice/record/phone-1/channel-1', '/device/jtDevice'],
    ['/channel/record/channel-1', '/channel'],
    ['/cloudRecord/detail/app-1/stream-1', '/cloudRecord']
  ])('keeps legacy or detail path %s associated with menu %s', async(routePath, activeMenu) => {
    const router = createRouter({ history: createMemoryHistory(), routes: navigationRoutes(constantRoutes) })

    await router.push(routePath)

    expect(router.currentRoute.value.meta.activeMenu || router.currentRoute.value.path).toBe(activeMenu)
  })
})
