import Vue from 'vue'

import 'normalize.css/normalize.css'

import ElementUI from 'element-ui'
import 'element-ui/lib/theme-chalk/index.css'

import '@/styles/index.scss'
import '@/assets/icons/iconfont.css'

import App from './App'
import store from './store'
import router from './router'

import '@/icons'
import '@/permission'

import perms from '@/directive/permission'

import { selectDictLabel } from '@/utils/data-process'

Vue.prototype.selectDictLabel = selectDictLabel

Vue.use(ElementUI)
Vue.use(perms)

Vue.config.productionTip = false

new Vue({
  el: '#app',
  router,
  store,
  render: h => h(App)
})
