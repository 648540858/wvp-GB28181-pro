import router from './router'
import store from './store'
import { Message } from 'element-ui'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { getToken } from '@/utils/auth'
import getPageTitle from '@/utils/get-page-title'

NProgress.configure({ showSpinner: false })

const whiteList = ['/login']

router.beforeEach(async(to, from, next) => {
  NProgress.start()

  document.title = getPageTitle(to.meta.title)

  const hasToken = getToken()

  if (hasToken) {
    if (to.path === '/login') {
      next({ path: '/' })
      NProgress.done()
    } else {
      // 加载菜单
      const hasMenus = store.getters.user_menus && store.getters.user_menus.length > 0
      if (hasMenus) {
        // 退出或刷新页面后重新动态加载路由
        const hasAddRoutes = store.getters.permission_add_routes && store.getters.permission_add_routes.length > 0
        if (hasAddRoutes) {
          next()
        } else {
          const accessRoutes = await store.dispatch('permission/generateRoutes')
          router.addRoutes(accessRoutes)
          next({ ...to, replace: true })
        }
      } else {
        // 请求用户菜单后动态加载路由
        try {
          await store.dispatch('user/getInfo')
          const accessRoutes = await store.dispatch('permission/generateRoutes')
          router.addRoutes(accessRoutes)
          next({ ...to, replace: true })
        } catch (error) {
          await store.dispatch('user/resetToken')
          Message.error(error || 'Has Error')
          next(`/login?redirect=${to.path}`)
          NProgress.done()
        }
      }
    }
  } else {
    if (whiteList.indexOf(to.path) !== -1) {
      next()
    } else {
      next(`/login?redirect=${to.path}`)
      NProgress.done()
    }
  }
})

router.afterEach(() => {
  NProgress.done()
})
