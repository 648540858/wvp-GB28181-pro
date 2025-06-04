import request from '@/utils/request'

export function pageApeDevice(params) {
  return request({
    url: '/api/viid/ape/device/page',
    method: 'get',
    params: params
  })
}

export function getDeviceOptions(params) {
  return request({
    url: '/api/viid/ape/device/page',
    method: 'get',
    headers: { showLoading: false },
    params: params
  })
}

export function getApeDevice(id) {
  return request({
    url: '/api/viid/ape/device/' + id,
    method: 'get'
  })
}

export function addApeDevice(data) {
  return request({
    url: '/api/viid/ape/device',
    method: 'post',
    data: data
  })
}

export function updateApeDevice(data) {
  return request({
    url: '/api/viid/ape/device',
    method: 'put',
    data: data
  })
}

export function delApeDevices(ids) {
  return request({
    url: '/api/viid/ape/device/' + ids,
    method: 'delete'
  })
}