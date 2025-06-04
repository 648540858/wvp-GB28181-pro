import { constantRoutes } from '@/router'
import Layout from '@/layout'
import Storage from 'good-storage'

const state = {
  routes: [],
  addRoutes: [],
  currentRoute: {}
}

const mutations = {
  SET_ROUTES: (state, routes) => {
    state.addRoutes = routes
    state.routes = constantRoutes.concat(routes)
  },
  SET_CURRENT_ROUTE: (state, route) => {
    state.currentRoute = route
  }
}

const actions = {
  generateRoutes({ commit }) {
    return new Promise(resolve => {
      const menus = Storage.get('data_ui_user_menus') || []
      const accessedRoutes = filterAsyncRouter(menus)
      accessedRoutes.push({ path: '*', redirect: '/404', hidden: true })
      commit('SET_ROUTES', accessedRoutes)
      resolve(accessedRoutes)
    })
  }
}

function filterAsyncRouter(routers) {
  const accessedRouters = routers.filter(router => {
    if (router.component) {
      if (router.component === 'Layout') {
        router.component = Layout
      } else {
        const component = router.component
        router.component = resolve => require(['@/views' + component], resolve)
      }
    }
    if (router.children && router.children.length) {
      router.children = filterAsyncRouter(router.children)
    }
    return true
  })
  return accessedRouters
}

export default {
  namespaced: true,
  state,
  mutations,
  actions
}
