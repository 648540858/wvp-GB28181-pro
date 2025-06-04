import request from '@/utils/request'

export function pagePublish(data) {
  return request({
    url: '/api/viid/publish/page',
    method: 'get',
    params: data
  })
}

export function getPublish(id) {
  return request({
    url: '/api/viid/publish/' + id,
    method: 'get'
  })
}

export function addPublish(data) {
  return request({
    url: '/api/viid/publish',
    method: 'post',
    data: data
  })
}

export function updatePublish(data) {
  return request({
    url: '/api/viid/publish',
    method: 'put',
    data: data
  })
}

export function delPublishs(ids) {
  return request({
    url: '/api/viid/publish/' + ids,
    method: 'delete'
  })
}