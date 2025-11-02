import axios from 'axios'
import { MessageBox, Message } from 'element-ui'
import store from '@/store'
import { getToken } from '@/utils/auth'

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
    if (response.config.url.indexOf('/api/user/logout') >= 0) {
      return
    }
    const res = response.data
    if (res.code && res.code !== 0) {
      Message.error({
        message: res.msg,
        duration: 5 * 1000
      })
    } else {
      return res
    }
  },
  error => {
    console.log(error) // for debug
    if (error.response.status === 401 && !showLoginConfirm && store.getters.showConfirmBoxForLoginLose) {
      // to re-login
      showLoginConfirm = true
      MessageBox.confirm('登录已经到期， 是否重新登录', '登录确认', {
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
    } else {
      if (!store.getters.showConfirmBoxForLoginLose) {
        return
      }
      let data = error.response.data
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
    // return Promise.reject(error)
  }
)

export default service
