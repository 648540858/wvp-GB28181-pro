import { createApp } from 'vue'

import 'normalize.css/normalize.css' // A modern alternative to CSS resets

import Antd from 'ant-design-vue'
import 'ant-design-vue/dist/reset.css'
import dayjs from 'dayjs'
import 'dayjs/locale/zh-cn'
import moment from 'moment'
import 'moment/locale/zh-cn'

import '@/styles/index.scss' // global css

import App from './App'
import store from './store'
import router from './router'

import '@/permission' // permission control
import { registerIcons } from '@/icons'
import antCompat from '@/components/antCompat'

dayjs.locale('zh-cn')
moment.locale('zh-cn')

const app = createApp(App)

app.config.globalProperties.$channelTypeList = {
  1: { id: 1, name: '国标设备', style: { color: '#409eff', borderColor: '#b3d8ff' } },
  2: { id: 2, name: '推流设备', style: { color: '#67c23a', borderColor: '#c2e7b0' } },
  3: { id: 3, name: '拉流代理', style: { color: '#e6a23c', borderColor: '#f5dab1' } },
  200: { id: 200, name: '部标设备', style: { color: '#fa6436', borderColor: '#f4997c' } }
}

app.use(store)
app.use(router)
app.use(Antd)
app.use(antCompat)
registerIcons(app)
app.mount('#app')
