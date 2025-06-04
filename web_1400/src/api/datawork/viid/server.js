import request from '@/utils/request'

export function pageServer(data) {
  return request({
    url: '/api/viid/server/page',
    method: 'get',
    params: data
  })
}

export function upsertServer(data) {
  return request({
    url: '/api/viid/server/upsert',
    method: 'post',
    data: data
  })
}

export function getServer(id) {
  return request({
    url: '/api/viid/server/' + id,
    method: 'get'
  })
}

export function delServers(ids) {
  return request({
    url: '/api/viid/server/' + ids,
    method: 'delete'
  })
}

export function changeServerEnable(data) {
  return request({
    url: '/api/viid/server/change/enable',
    method: 'put',
    data: data
  })
}

export function changeServerKeepalive(data) {
  return request({
    url: '/api/viid/server/change/keepalive',
    method: 'put',
    data: data
  })
}

export function getCurrentServer() {
  return request({
    url: '/api/viid/server/me',
    method: 'get',
  })
}

export function updateCurrentServer(data) {
  return request({
    url: '/api/viid/server/me',
    method: 'post',
    data: data
  })
}