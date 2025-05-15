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

export function deleteDevice(phoneNumber) {
  return request({
    method: 'delete',
    url: '/api/jt1078/terminal/delete',
    params: {
      phoneNumber: phoneNumber
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

export function play(params) {
  const { phoneNumber, channelId, type } = params
  return request({
    method: 'get',
    url: '/api/jt1078/live/start',
    params: {
      phoneNumber: phoneNumber,
      channelId: channelId,
      type: type
    }
  })
}
export function stopPlay(params) {
  const { phoneNumber, channelId } = params
  return request({
    method: 'get',
    url: '/api/jt1078/live/stop',
    params: {
      phoneNumber: phoneNumber,
      channelId: channelId
    }
  })
}
export function updateChannel(data) {
  return request({
    method: 'post',
    url: `/api/jt1078/terminal/channel/update`,
    data: data
  })
}
export function addChannel(data) {
  return request({
    method: 'post',
    url: `/api/jt1078/terminal/channel/add`,
    data: data
  })
}

export function ptz(params) {
  const { phoneNumber, channelId, command, speed } = params
  return request({
    method: 'get',
    url: '/api/jt1078/ptz',
    params: {
      phoneNumber: phoneNumber,
      channelId: channelId,
      command: command,
      speed: speed
    }
  })
}
export function wiper(params) {
  const { phoneNumber, channelId, command } = params
  return request({
    method: 'get',
    url: '/api/jt1078/wiper',
    params: {
      phoneNumber: phoneNumber,
      channelId: channelId,
      command: command
    }
  })
}
export function fillLight(params) {
  const { phoneNumber, channelId, command } = params
  return request({
    method: 'get',
    url: '/api/jt1078/fill-light',
    params: {
      phoneNumber: phoneNumber,
      channelId: channelId,
      command: command
    }
  })
}

