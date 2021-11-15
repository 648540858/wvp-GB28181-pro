import request from '@/utils/request'

const userApi = {
  UserMenu: '/user/nav'
}

export function getCurrentUserNav () {
  return request({
    url: userApi.UserMenu,
    method: 'get'
  })
}
//登录
export function toLogin (parameter) {
  return request({
    url: '/api/user/login',
    method: 'get',
    params: parameter
  })
}

//注销
export function logout () {
  return request({
    url: '/api/user/logout',
    method: 'get',
  })
}

//修改密码
export function changePassword (parameter) {
  return request({
    url: '/api/user/changePassword',
    method: 'post',
    params: parameter
  })
}
