import request from '@/utils/request'

export function pageDatareload(data) {
  return request({
    url: '/api/dwn/data/reload/page',
    method: 'get',
    params: data
  })
}

export function getDatareload(id) {
  return request({
    url: '/api/dwn/data/reload/' + id,
    method: 'get'
  })
}

export function delDatareload(id) {
  return request({
    url: '/api/dwn/data/reload/' + id,
    method: 'delete'
  })
}

export function delDatareloads(ids) {
  return request({
    url: '/api/dwn/data/reload/batch/' + ids,
    method: 'delete'
  })
}

export function addDatareload(data) {
  return request({
    url: '/api/dwn/data/reload',
    method: 'post',
    data: data
  })
}

export function updateDatareload(data) {
  return request({
    url: '/api/dwn/data/reload',
    method: 'put',
    data: data
  })
}

export function executeDateReload(id) {
  return request({
    url: '/api/dwn/data/reload/execute/' + id,
    method: 'get'
  })
}

export function changeDateReloadStatus(params) {
  return request({
    url: '/api/dwn/data/reload/running',
    method: 'put',
    params: params
  })
}

