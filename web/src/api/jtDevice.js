import request from '@/utils/request'

// 部标设备API

export function queryDevices({ page, count, query, online }) {
  return request({
    method: 'get',
    url: '/api/jt1078/terminal/list',
    params: {
      page: page,
      count: count,
      query: query,
      online: online
    }
  })
}

export function queryDeviceById(deviceId) {
  return request({
    method: 'get',
    url: '/api/jt1078/terminal/query',
    params: {
      deviceId: deviceId
    }
  })
}

export function update(form) {
  return request({
    method: 'post',
    url: '/api/jt1078/terminal/update',
    params: form
  })
}

export function add(form) {
  return request({
    method: 'post',
    url: '/api/jt1078/terminal/add',
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
    url: '/api/jt1078/terminal/channel/list',
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
    url: '/api/jt1078/terminal/channel/update',
    data: data
  })
}
export function addChannel(data) {
  return request({
    method: 'post',
    url: '/api/jt1078/terminal/channel/add',
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
export function queryRecordList(params) {
  const { phoneNumber, channelId, startTime, endTime } = params
  return request({
    method: 'get',
    url: '/api/jt1078/record/list',
    params: {
      phoneNumber: phoneNumber,
      channelId: channelId,
      startTime: startTime,
      endTime: endTime
    }
  })
}
export function startPlayback(params) {
  const { phoneNumber, channelId, startTime, endTime, type, rate, playbackType, playbackSpeed } = params
  return request({
    method: 'get',
    url: '/api/jt1078/playback/start/',
    params: {
      phoneNumber: phoneNumber,
      channelId: channelId,
      startTime: startTime,
      endTime: endTime,
      type: type,
      rate: rate,
      playbackType: playbackType,
      playbackSpeed: playbackSpeed
    }
  })
}
export function getRecordTempUrl({ phoneNumber, channelId, startTime, endTime, alarmSign, mediaType, streamType, storageType }) {
  return request({
    method: 'get',
    url: '/api/jt1078/playback/downloadUrl',
    params: {
      phoneNumber: phoneNumber,
      channelId: channelId,
      startTime: startTime,
      endTime: endTime,
      alarmSign: alarmSign,
      mediaType: mediaType,
      streamType: streamType,
      storageType: storageType
    }
  })
}
export function controlPlayback(params) {
  const { phoneNumber, channelId, command, playbackSpeed, time } = params
  return request({
    method: 'get',
    url: '/api/jt1078/playback/control',
    params: {
      phoneNumber: phoneNumber,
      channelId: channelId,
      command: command,
      playbackSpeed: playbackSpeed,
      time: time
    }
  })
}
export function stopPlayback(params) {
  const { phoneNumber, channelId, streamId } = params
  return request({
    method: 'get',
    url: '/api/jt1078/playback/stop/',
    params: {
      phoneNumber: phoneNumber,
      channelId: channelId,
      streamId: streamId
    }
  })
}
export function queryConfig(phoneNumber) {
  return request({
    method: 'get',
    url: '/api/jt1078/config',
    params: {
      phoneNumber: phoneNumber
    }
  })
}
export function setConfig(data) {
  return request({
    method: 'post',
    url: '/api/jt1078/set-config',
    data: data
  })
}
export function queryAttribute(phoneNumber) {
  return request({
    method: 'get',
    url: '/api/jt1078/attribute',
    params: {
      phoneNumber: phoneNumber
    }
  })
}
export function linkDetection(phoneNumber) {
  return request({
    method: 'get',
    url: '/api/jt1078/link-detection',
    params: {
      phoneNumber: phoneNumber
    }
  })
}
export function queryPosition(phoneNumber) {
  return request({
    method: 'get',
    url: '/api/jt1078/position-info',
    params: {
      phoneNumber: phoneNumber
    }
  })
}
export function sendTextMessage(data) {
  return request({
    method: 'post',
    url: '/api/jt1078/text-msg',
    data: data
  })
}
export function telephoneCallback({ phoneNumber, sign, destPhoneNumber }) {
  return request({
    method: 'get',
    url: '/api/jt1078/telephone-callback',
    params: {
      phoneNumber: phoneNumber,
      sign: sign,
      destPhoneNumber: destPhoneNumber
    }
  })
}
export function queryDriverInfo(phoneNumber) {
  return request({
    method: 'get',
    url: '/api/jt1078/driver-information',
    params: {
      phoneNumber: phoneNumber
    }
  })
}
export function factoryReset(phoneNumber) {
  return request({
    method: 'post',
    url: '/api/jt1078/control/factory-reset',
    params: {
      phoneNumber: phoneNumber
    }
  })
}
export function reset(phoneNumber) {
  return request({
    method: 'post',
    url: '/api/jt1078/control/reset',
    params: {
      phoneNumber: phoneNumber
    }
  })
}
export function connection(data) {
  return request({
    method: 'post',
    url: '/api/jt1078/control/connection',
    data: data
  })
}
export function controlDoor({ phoneNumber, open}) {
  return request({
    method: 'get',
    url: '/api/jt1078/control/door',
    params: {
      phoneNumber: phoneNumber,
      open: open
    }
  })
}


