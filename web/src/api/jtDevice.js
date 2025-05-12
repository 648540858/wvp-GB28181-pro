import request from '@/utils/request'

// 部标设备API

export function queryDevices(params) {
  const { page, count } = params
  return request({
    method: 'get',
    url: `/api/jt1078/terminal/list`,
    params: {
      page: page,
      count: count
    }
  })
}

export function queryDeviceById(deviceId) {
  return request({
    method: 'get',
    url: `/api/jt1078/terminal/query`,
    params: {
      deviceId: deviceId
    }
  })
}

export function update(form) {
  return request({
    method: 'post',
    url: `/api/jt1078/terminal/update`,
    params: form
  })
}

export function add(form) {
  return request({
    method: 'post',
    url: `/api/jt1078/terminal/add`,
    params: form
  })
}

export function deleteDeviceById(deviceId) {
  return request({
    method: 'delete',
    url: '/api/jt1078/terminal/delete',
    params: {
      deviceId: deviceId
    }
  })
}

export function queryChannels(params) {
  const { page, count, query, deviceId } = params
  return request({
    method: 'get',
    url: `/api/jt1078/terminal/channel/list`,
    params: {
      page: page,
      count: count,
      query: query,
      deviceId: deviceId
    }
  })
}

