import request from '@/utils/request'

export function pageDisposition(params) {
  return request({
    url: '/api/viid/disposition/page',
    method: 'get',
    params: params
  })
}

export function getDisposition(id) {
  return request({
    url: '/api/viid/disposition/' + id,
    method: 'get'
  })
}

export function addDisposition(data) {
  return request({
    url: '/api/viid/disposition',
    method: 'post',
    data: data
  })
}

export function updateDisposition(data) {
  return request({
    url: '/api/viid/disposition',
    method: 'put',
    data: data
  })
}

export function delDispositions(ids) {
  return request({
    url: '/api/viid/disposition/' + ids,
    method: 'delete'
  })
}
