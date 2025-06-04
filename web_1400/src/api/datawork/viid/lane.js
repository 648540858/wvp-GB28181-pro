import request from '@/utils/request'

export function pageLane(data) {
  return request({
    url: '/api/viid/lane/page',
    method: 'get',
    params: data
  })
}

export function getLane(id) {
  return request({
    url: '/api/viid/lane/' + id,
    method: 'get',
  })
}

export function addLane(data) {
  return request({
    url: '/api/viid/lane',
    method: 'post',
    data: data
  })
}

export function updateLane(data) {
  return request({
    url: '/api/viid/lane',
    method: 'put',
    data: data
  })
}

export function delLanes(ids) {
  return request({
    url: '/api/viid/lane/' + ids,
    method: 'delete'
  })
}