import Vue from 'vue'
import VueRouter from 'vue-router'

import control from '../components/control.vue'
import deviceList from '../components/DeviceList.vue'
import channelList from '../components/channelList.vue'
import devicePosition from  '../components/devicePosition.vue'
import login from '../components/Login.vue'
import parentPlatformList from '../components/ParentPlatformList.vue'

const originalPush = VueRouter.prototype.push
VueRouter.prototype.push = function push(location) {
  return originalPush.call(this, location).catch(err => err)
}

Vue.use(VueRouter)


export default new VueRouter({
  mode:'hash',
  routes: [
    {
      path: '/',
      component: control,
    },
    {
      path: '/deviceList',
      component: deviceList,
    },
    {
      path: '/login',
      name: '登录',
      component: login,
    },
    {
      path: '/channelList/:deviceId/:parentChannelId/:count/:page',
      name: 'channelList',
      component: channelList,
    },
    {
      path: '/parentPlatformList/:count/:page',
      name: 'parentPlatformList',
      component: parentPlatformList,
    },
    {
      path: '/devicePosition/:deviceId/:parentChannelId/:count/:page',
      name: 'devicePosition',
      component: devicePosition,
    },
  ]
})
