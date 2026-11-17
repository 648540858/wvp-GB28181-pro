import axios from 'axios'
import { Message } from 'element-ui'
import store from '@/store'
import { getToken } from '@/utils/auth'

// 登录过期只处理一次，避免并发请求各自弹窗/各自跳转
let isRelogin = false

// create an axios instance
const service = axios.create({
  baseURL: process.env.VUE_APP_BASE_API, // url = base url + request url
  // withCredentials: true, // send cookies when cross-domain requests
  timeout: 30000 // request timeout
})

// request interceptor
service.interceptors.request.use(
  config => {
    // do something before request is sent
    if (store.getters.token && config.url.indexOf('api/user/login') < 0) {
      config.headers['access-token'] = getToken()
    }
    return config
  },
  error => {
    // do something with request error
    console.log(error) // for debug
    return Promise.reject(error)
  }
)

// response interceptor
service.interceptors.response.use(
  /**
   * If you want to get http information such as headers or status
   * Please return  response => response
  */

  /**
   * Determine the request status by custom code
   * Here is just an example
   * You can also judge the status by HTTP Status Code
   */
  response => {
    if (response.config.url.indexOf('/api/user/logout') >= 0) {
      return
    }
    const res = response.data
    if (res.code && res.code !== 0) {
      throw res.msg
    } else {
      return res
    }
  },
  error => {
    console.log(error) // for debug
    const response = error.response
    if (response && response.status === 401) {
      // 后端 JWT 已过期：清掉本地 token 后回到登录页。
      // 不要弹确认框 —— 页面上的轮询接口会持续失败，导致刷屏。
      if (!isRelogin) {
        isRelogin = true
        Message.warning({
          message: '登录已过期，请重新登录',
          showClose: true
        })
        store.dispatch('user/resetToken').finally(() => {
          // 留一点时间让提示可见；reload 后 permission.js 会带上 redirect 跳到登录页
          setTimeout(() => {
            window.location.reload()
          }, 800)
        })
      }
    } else if (store.getters.showConfirmBoxForLoginLose) {
      const data = response && response.data
      if (data && data.msg) {
        Message.error({
          message: data.msg,
          showClose: true
        })
      }else {
        Message.error({
          message: error.message,
          showClose: true
        })
      }
    }
    // 必须把错误继续抛出去，否则调用方会拿到 undefined，
    // 进而在 response.data 处报 "Cannot read properties of undefined"
    return Promise.reject(error)
  }
)

export default service
