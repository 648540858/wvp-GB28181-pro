import request from '@/utils/request'

export function pageSubscribe(data) {
  return request({
    url: '/api/viid/subscribe/page',
    method: 'get',
    params: data
  })
}

export function getSubscribe(id) {
  return request({
    url: '/api/viid/subscribe/' + id,
    method: 'get'
  })
}

export function addSubscribe(data) {
  return request({
    url: '/api/viid/subscribe',
    method: 'post',
    data: data
  })
}

export function delSubscribes(ids) {
  return request({
    url: '/api/viid/subscribe/' + ids,
    method: 'delete'
  })
}