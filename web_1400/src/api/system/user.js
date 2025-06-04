import request from '@/utils/request'
import qs from 'qs'

export function login(username, password) {
  const data = {
    username: username,
    password: password
  }
  return request({
    url: '/api/admin/login',
    widthCredentials: true,
    method: 'post',
    data: qs.stringify(data)
  })
}

export function getRoutes() {
  const timstamp = new Date().getTime()
  return request({
    url: '/api/admin/route?t=' + timstamp,
    method: 'get'
  })
}

export function logout(token) {
  return request({
    url: `/api/admin/logout`,
    method: 'delete'
  })
}

export function log() {
  return request({
    url: '/api/admin/login/logs',
    method: 'post'
  })
}

