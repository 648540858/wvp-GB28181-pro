import request from '@/utils/request'

export function login(params) {
  return request({
    url: '/api/user/login',
    method: 'get',
    params: params
  })
}

export function logout() {
  return request({
    url: '/api/user/logout',
    method: 'get'
  })
}
export function getUserInfo() {
  return request({
    method: 'post',
    url: '/api/user/userInfo'
  })
}

export function changePushKey(params) {
  const { pushKey, userId } = params
  return request({
    method: 'post',
    url: '/api/user/changePushKey',
    params: {
      pushKey: pushKey,
      userId: userId
    }
  })
}

export function queryList(params) {
  const { page, count } = params
  return request({
    method: 'get',
    url: `/api/user/users`,
    params: {
      page: page,
      count: count
    }
  })
}

export function removeById(id) {
  return request({
    method: 'delete',
    url: `/api/user/delete?id=${id}`

  })
}

export function add(params) {
  const { username, password, roleId } = params
  return request({
    method: 'post',
    url: '/api/user/add',
    params: {
      username: username,
      password: password,
      roleId: roleId
    }
  })
}

export function changePassword(params) {
  const { oldPassword, password } = params
  return request({
    method: 'post',
    url: '/api/user/changePassword',
    params: {
      oldPassword: oldPassword,
      password: password
    }
  })
}

export function changePasswordForAdmin(params) {
  const { password, userId } = params
  return request({
    method: 'post',
    url: '/api/user/changePasswordForAdmin',
    params: {
      password: password,
      userId: userId
    }
  })
}
