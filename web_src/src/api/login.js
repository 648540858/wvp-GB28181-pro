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
