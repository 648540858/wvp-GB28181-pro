import Vue from 'vue'
import VueRouter from 'vue-router'

import control from '../components/control.vue'
import deviceList from '../components/DeviceList.vue'
import channelList from '../components/channelList.vue'
import pushVideoList from '../components/PushVideoList.vue'
import streamProxyList from '../components/StreamProxyList.vue'
import devicePosition from  '../components/devicePosition.vue'
import login from '../components/Login.vue'
import parentPlatformList from '../components/ParentPlatformList.vue'
import cloudRecord from '../components/CloudRecord.vue'
import mediaServerManger from '../components/MediaServerManger.vue'
import test from '../components/test.vue'
import web from '../components/setting/Web.vue'
import sip from '../components/setting/Sip.vue'
import media from '../components/setting/Media.vue'
import live from '../components/live.vue'

import wasmPlayer from '../components/dialog/jessibuca.vue'
import rtcPlayer from '../components/dialog/rtcPlayer.vue'

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
      path: '/live',
      component: live,
    },
    {
      path: '/deviceList',
      component: deviceList,
    },
    {
      path: '/pushVideoList',
      component: pushVideoList,
    },
    {
      path: '/streamProxyList',
      component: streamProxyList,
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
    {
      path: '/cloudRecord',
      name: 'cloudRecord',
      component: cloudRecord,
    },
    {
      path: '/mediaServerManger',
      name: 'mediaServerManger',
      component: mediaServerManger,
    },
    {
      path: '/setting/web',
      name: 'web',
      component: web,
    },
    {
      path: '/setting/sip',
      name: 'sip',
      component: sip,
    },
    {
      path: '/setting/media',
      name: 'media',
      component: media,
    },
    {
      path: '/test',
      name: 'test',
      component: test,
    },
    {
      path: '/play/wasm/:url',
      name: 'wasmPlayer',
      component: wasmPlayer,
    },
    {
      path: '/play/rtc/:url',
      name: 'rtcPlayer',
      component: rtcPlayer,
    },
  ]
})
