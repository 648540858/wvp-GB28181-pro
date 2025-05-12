import Vue from 'vue'
import Router from 'vue-router'

Vue.use(Router)

/* Layout */
import Layout from '@/layout'

/**
 * Note: sub-menu only appear when route children.length >= 1
 * Detail see: https://panjiachen.github.io/vue-element-admin-site/guide/essentials/router-and-nav.html
 *
 * hidden: true                   if set true, item will not show in the sidebar(default is false)
 * alwaysShow: true               if set true, will always show the root menu
 *                                if not set alwaysShow, when item has more than one children route,
 *                                it will becomes nested mode, otherwise not show the root menu
 * redirect: noRedirect           if set noRedirect will no redirect in the breadcrumb
 * name:'router-name'             the name is used by <keep-alive> (must set!!!)
 * meta : {
 roles: ['admin','editor']    control the page roles (you can set multiple roles)
 title: 'title'               the name show in sidebar and breadcrumb (recommend set)
 icon: 'svg-name'/'el-icon-x' the icon show in the sidebar
 breadcrumb: false            if set false, the item will hidden in breadcrumb(default is true)
 activeMenu: '/example/list'  if set path, the sidebar will highlight the path you set
 }
 */

/**
 * constantRoutes
 * a base page that does not have permission requirements
 * all roles can be accessed
 */
export const constantRoutes = [
  {
    path: '/login',
    component: () => import('@/views/login/index'),
    hidden: true
  },

  {
    path: '/404',
    component: () => import('@/views/404'),
    hidden: true
  },

  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: [{
      path: 'dashboard',
      name: '控制台',
      component: () => import('@/views/dashboard/index'),
      meta: { title: '控制台', icon: 'dashboard', affix: true }
    }]
  },

  {
    path: '/live',
    component: Layout,
    redirect: '/live',
    children: [{
      path: '',
      name: 'Live',
      component: () => import('@/views/live/index'),
      meta: { title: '分屏监控', icon: 'live' }
    }]
  },
  {
    path: '/device',
    component: Layout,
    redirect: '/device',
    onlyIndex: 0,
    children: [
      {
        path: '',
        name: 'Device',
        component: () => import('@/views/device/index'),
        meta: { title: '国标设备', icon: 'device' }
      },
      {
        path: '/device/record/:deviceId/:channelDeviceId',
        name: 'DeviceRecord',
        component: () => import('@/views/device/channel/record'),
        meta: { title: '国标录像' }
      }
    ]
  },
  {
    path: '/push',
    component: Layout,
    redirect: '/push',
    children: [
      {
        path: '',
        name: 'PushList',
        component: () => import('@/views/streamPush/index'),
        meta: { title: '推流列表', icon: 'streamPush' }
      }
    ]
  },
  {
    path: '/proxy',
    component: Layout,
    redirect: '/proxy',
    children: [
      {
        path: '',
        name: 'Proxy',
        component: () => import('@/views/streamProxy/index'),
        meta: { title: '拉流代理', icon: 'streamProxy' }
      }
    ]
  },
  {
    path: '/commonChannel',
    component: Layout,
    redirect: '/commonChannel/region',
    name: '通道管理',
    meta: { title: '通道管理', icon: 'channelManger' },
    children: [
      {
        path: 'region',
        name: 'Region',
        component: () => import('@/views/channel/region/index'),
        meta: { title: '行政区划', icon: 'region' }
      },
      {
        path: 'group',
        name: 'Group',
        component: () => import('@/views/channel/group/index'),
        meta: { title: '业务分组', icon: 'tree' }
      }
    ]
  },
  {
    path: '/recordPlan',
    component: Layout,
    redirect: '/recordPlan',
    children: [
      {
        path: '',
        name: 'RecordPlan',
        component: () => import('@/views/recordPlan/index'),
        meta: { title: '录制计划', icon: 'recordPlan' }
      }
    ]
  },
  {
    path: '/cloudRecord',
    component: Layout,
    redirect: '/cloudRecord',
    onlyIndex: 0,
    children: [
      {
        path: '/cloudRecord',
        name: 'CloudRecord',
        component: () => import('@/views/cloudRecord/index'),
        meta: { title: '云端录像', icon: 'cloudRecord' }
      },
      {
        path: '/cloudRecord/detail/:app/:stream',
        name: 'CloudRecordDetail',
        component: () => import('@/views/cloudRecord/detail'),
        meta: { title: '云端录像详情' }
      }
    ]
  },
  {
    path: '/mediaServer',
    component: Layout,
    redirect: '/mediaServer',
    children: [
      {
        path: '',
        name: 'MediaServer',
        component: () => import('@/views/mediaServer/index'),
        meta: { title: '媒体节点', icon: 'mediaServerList' }
      }
    ]
  },
  {
    path: '/platform',
    component: Layout,
    redirect: '/platform',
    children: [
      {
        path: '',
        name: 'Platform',
        component: () => import('@/views/platform/index'),
        meta: { title: '国标级联', icon: 'platform' }
      }
    ]
  },
  {
    path: '/user',
    component: Layout,
    redirect: '/user',
    children: [
      {
        path: '',
        name: 'User',
        component: () => import('@/views/user/index'),
        meta: { title: '用户管理', icon: 'user' }
      }
    ]
  },
  // {
  //   path: '/setting',
  //   component: Layout,
  //   redirect: '/setting',
  //   children: [
  //     {
  //       path: '',
  //       name: '系统设置',
  //       component: () => import('@/views/platform/index'),
  //       meta: { title: '系统设置', icon: 'setting' }
  //     }
  //   ]
  // },
  {
    path: '/operations',
    component: Layout,
    meta: { title: '运维中心', icon: 'operations' },
    redirect: '/operations/systemInfo',
    children: [
      {
        path: '/operations/systemInfo',
        name: 'OperationsSystemInfo',
        component: () => import('@/views/operations/systemInfo'),
        meta: { title: '平台信息', icon: 'systemInfo' }
      },
      {
        path: '/operations/historyLog',
        name: 'OperationsHistoryLog',
        component: () => import('@/views/operations/historyLog'),
        meta: { title: '历史日志', icon: 'historyLog' }
      },
      {
        path: '/operations/realLog',
        name: 'OperationsRealLog',
        component: () => import('@/views/operations/realLog'),
        meta: { title: '实时日志', icon: 'realLog' }
      }
    ]
  },
  {
    path: '/play/wasm/:url',
    name: 'wasmPlayer',
    hidden: true,
    component: () => import('@/views/common/jessibuca.vue')
  },
  {
    path: '/play/rtc/:url',
    name: 'rtcPlayer',
    component: () => import('@/views/common/rtcPlayer.vue')
  },
  // 404 page must be placed at the end !!!
  { path: '*', redirect: '/404', hidden: true }
]

const createRouter = () => new Router({
  // mode: 'history', // require service support
  scrollBehavior: () => ({ y: 0 }),
  routes: constantRoutes
})

const router = createRouter()

// Detail see: https://github.com/vuejs/vue-router/issues/1234#issuecomment-357941465
export function resetRouter() {
  const newRouter = createRouter()
  router.matcher = newRouter.matcher // reset router
}

export default router
