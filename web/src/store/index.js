import Vue from 'vue'
import Vuex from 'vuex'
import getters from './getters'
import app from './modules/app'
import settings from './modules/settings'
import user from './modules/user'
import tagsView from './modules/tagsView'
import commonChanel from './modules/commonChanel'
import region from './modules/region'
import device from './modules/device'
import group from './modules/group'
import server from './modules/server'
import play from './modules/play'
import playback from './modules/playback'
import streamPush from './modules/streamPush'
import streamProxy from './modules/streamProxy'
import recordPlan from './modules/recordPlan'
import cloudRecord from './modules/cloudRecord'
import platform from './modules/platform'
import role from './modules/role'
import userApiKeys from './modules/userApiKeys'
import gbRecord from './modules/gbRecord'
import log from './modules/log'
import frontEnd from './modules/frontEnd'

Vue.use(Vuex)

const store = new Vuex.Store({
  modules: {
    app,
    settings,
    user,
    tagsView,
    commonChanel,
    region,
    device,
    group,
    server,
    play,
    playback,
    streamPush,
    streamProxy,
    recordPlan,
    cloudRecord,
    platform,
    role,
    userApiKeys,
    gbRecord,
    log,
    frontEnd
  },
  getters
})

export default store
