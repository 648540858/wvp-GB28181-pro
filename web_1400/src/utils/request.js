import axios from 'axios'
import { Loading, Message } from 'element-ui'
import store from '@/store'
import router from '@/router'

// create an axios instance
const service = axios.create({
  baseURL: process.env.VUE_APP_BASE_API,
  timeout: 10000
})

let loadingInstance

// request interceptor
service.interceptors.request.use(
  config => {
    const hasToken = store.getters.token
    if (hasToken) {
      config.headers['Authorization'] = 'Bearer ' + hasToken
    }
    if (config.headers.showLoading !== false) {
      loadingInstance = Loading.service({
        lock: true,
        text: '数据加载中，请稍后...',
        spinner: 'el-icon-loading',
        background: 'rgba(0, 0, 0, 0.7)'
      })
    }
    return config
  },
  error => {
    if (error.config.headers.showLoading !== false) {
      loadingInstance.close()
    }
    Message.error('请求错误')
    console.log(error)
    return Promise.reject(error)
  }
)

// response interceptor
service.interceptors.response.use(
  response => {
    if (response.config.headers.showLoading !== false) {
      loadingInstance.close()
    }
    const code = response.data.code || 200
    if (code === 500) {
      Message.error(response.data.msg || '系统错误')
      return Promise.reject(new Error(response.data.msg))
    } else {
      return response.data
    }
  },
  error => {
    if (error.config.headers.showLoading !== false) {
      loadingInstance.close()
    }
    if (error.response.status) {
      switch (error.response.status) {
        // 401: 未登录
        // 未登录则跳转登录页面，并携带当前页面的路径
        // 在登录成功后返回当前页面，这一步需要在登录页操作。
        case 401:
          Message.error(error.response.data.msg || '系统错误')
          store.dispatch('user/resetToken')
          router.replace({
            path: '/login',
            query: { redirect: router.currentRoute.fullPath }
          })
          break
        // 403 token过期
        // 登录过期对用户进行提示
        // 清除本地token和清空vuex中token对象
        // 跳转登录页面
        case 403:
          Message.error('登录过期，请重新登录')
          // 清除token
          store.dispatch('user/resetToken')
          // 跳转登录页面，并将要浏览的页面fullPath传过去，登录成功后跳转需要访问的页面
          setTimeout(() => {
            router.replace({
              path: '/login',
              query: { redirect: router.currentRoute.fullPath }
            })
          }, 1000)
          break
        // 404请求不存在
        case 404:
          Message.error('网络请求不存在')
          break
        // 其他错误，直接抛出错误提示
        default:
          Message.error(error.response.data.msg)
      }
    }
    return Promise.reject(error.response)
  }
)

export default service
