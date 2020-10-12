import Vue from 'vue'
import VueRouter from 'vue-router'

import control from '../components/control.vue'
import videoList from '../components/videoList.vue'
import channelList from '../components/channelList.vue'
import login from '../components/Login.vue'

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
      path: '/videoList',
      component: videoList,
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
  ]
})
