import axios from 'axios'
import { MessageBoxConfirm, Message } from '@/components/antCompat'
import store from '@/store'
import { getToken, setToken } from '@/utils/auth'

let showLoginConfirm = false

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
    const renewedToken = response.headers && response.headers['access-token']
    if (renewedToken) {
      store.commit('user/SET_TOKEN', renewedToken)
      setToken(renewedToken)
    }
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
    const requestUrl = error.config && error.config.url ? error.config.url : ''
    const isLoginRequest = requestUrl.indexOf('/api/user/login') >= 0

    if (response && response.status === 401) {
      if (!isLoginRequest && !showLoginConfirm && store.getters.showConfirmBoxForLoginLose) {
        // to re-login
        showLoginConfirm = true
        MessageBoxConfirm('登录已经到期， 是否重新登录', '登录确认', {
          confirmButtonText: '重新登录',
          cancelButtonText: '取消',
          type: 'warning'
        }).then(() => {
          store.dispatch('user/resetToken').then(() => {
            location.reload()
          })
        }).catch(() => {
          store.dispatch('user/closeConfirmBoxForLoginLose')
          Message.warning({
            type: 'warning',
            message: '登录过期提示已经关闭，请注销后重新登录'
          })
          // 清除token， 后续请求不再继续

        })
      }
    } else if (!isLoginRequest && store.getters.showConfirmBoxForLoginLose) {
      const data = response && response.data
      if (data && data.msg) {
        Message.error({
          message: data.msg,
          showClose: true
        })
      } else {
        Message.error({
          message: error.message || '请求失败，请稍后重试',
          showClose: true
        })
      }
    }
    return Promise.reject(error)
  }
)

export default service
