// eslint-disable-next-line
import {UserLayout, BasicLayout, BlankLayout} from '@/layouts'
import {bxAnaalyse} from '@/core/icons'

const RouteView = {
  name: 'RouteView',
  render: h => h('router-view')
}

export const asyncRouterMap = [
  {
    path: '/',
    name: 'index',
    component: BasicLayout,
    meta: {title: 'menu.home'},
    redirect: '/dashboard/monitor',
    children: [
      // monitor
      {
        path: '/dashboard',
        name: 'dashboard',
        redirect: '/dashboard/monitor',
        component: RouteView,
        meta: {title: 'menu.dashboard', keepAlive: true, icon: bxAnaalyse},
        children: [
          {
            path: '/dashboard/monitor',
            name: 'Monitor',
            component: () => import('@/views/dashboard/MonitorIndex'),
            meta: {title: 'menu.dashboard.monitor', keepAlive: false}
          }
        ]
      },
      // Video Devices Manage
      {
        path: '/videoMatrix',
        name: 'videoMatrix',
        component: RouteView,
        redirect: '/video/videoMatrix',
        meta: {title: 'menu.video.video-matrix', icon: 'appstore'},
        children: [
          {
            path: '/video/videoMatrix',
            name: 'videoMatrixSquare',
            component: () => import(/* webpackChunkName: "fail" */ '@/views/videoSquare/squareIndex'),
            meta: {title: 'menu.video.square-matrix'}
          }
        ]
      },
      {
        path: '/deviceManage',
        name: 'deviceManage',
        component: RouteView,
        redirect: '/video/deviceList',
        meta: {title: 'menu.video.device-manage', icon: 'video-camera'},
        children: [
          {
            path: '/video/deviceList',
            name: 'deviceList',
            component: () => import(/* webpackChunkName: "fail" */ '@/views/device/DeviceIndex'),
            meta: {title: 'menu.video.device-list'}
          }
        ]
      },
      // Video Records Manage
      {
        path: '/recordManage',
        name: 'recordManage',
        component: RouteView,
        redirect: '/video/recordList',
        meta: {title: 'menu.video.record-manage', icon: 'unordered-list'},
        children: [
          {
            path: '/video/recordList',
            name: 'recordList',
            component: () => import(/* webpackChunkName: "fail" */ '@/views/records/RecordIndex'),
            meta: {title: 'menu.video.record-list'}
          },
          {
            path: '/video/nvrRecordIndex',
            name: 'nvrRecordIndex',
            component: () => import(/* webpackChunkName: "fail" */ '@/views/records/NVRRecordIndex'),
            meta: {title: 'menu.video.nvr-record-list'}
          }
        ]
      },
      {
        path: '/deviceWarning',
        name: 'deviceWarning',
        component: RouteView,
        redirect: '/device/warning',
        meta: {title: 'menu.video.device-warning-manage', icon: 'bell'},
        children: [
          {
            path: '/device/warning',
            name: 'warningList',
            component: () => import(/* webpackChunkName: "fail" */ '@/views/warning/WarningList'),
            meta: {title: 'menu.video.device-warning-list'}
          }
        ]
      },
      {
        path: '/streamProxy',
        name: 'streamProxy',
        component: RouteView,
        redirect: '/stream/proxy',
        meta: {title: 'menu.video.stream-proxy-manage', icon: 'deployment-unit'},
        children: [
          {
            path: '/stream/proxy',
            name: 'streamProxyList',
            component: () => import(/* webpackChunkName: "fail" */ '@/views/streamProxy/StreamProxyList'),
            meta: {title: 'menu.video.stream-proxy-list'}
          }
        ]
      },
      {
        path: '/gbPlatform',
        name: 'gbPlatform',
        component: RouteView,
        redirect: '/gbPlatform/index',
        meta: {title: 'menu.video.gbPlatform-manage', icon: 'apartment'},
        children: [
          {
            path: '/gbPlatform/index',
            name: 'gbPlatformIndex',
            component: () => import(/* webpackChunkName: "fail" */ '@/views/gbPlatform/GbPlatformIndex'),
            meta: {title: 'menu.video.gbPlatform-list'}
          }
        ]
      },
      {
        path: '/mediaServer',
        name: 'mediaServer',
        component: RouteView,
        redirect: '/mediaServer/index',
        meta: {title: 'menu.video.media-server-manage', icon: 'cloud-server'},
        children: [
          {
            path: '/mediaServer/index',
            name: 'mediaServerIndex',
            component: () => import(/* webpackChunkName: "fail" */ '@/views/mediaServer/MediaServerIndex'),
            meta: {title: 'menu.video.media-server-list'}
          }
        ]
      }
    ]
  },
  {
    path: '*',
    redirect: '/404',
    hidden: true
  }
]

/**
 * 基础路由
 * @type { *[] }
 */
export const constantRouterMap = [
  {
    path: '/404',
    component: () => import(/* webpackChunkName: "fail" */ '@/views/exception/404')
  }
]
