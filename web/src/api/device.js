import request from '@/utils/request'

// 国标设备API

export function queryDeviceSyncStatus(deviceId) {
  return request({
    method: 'get',
    url: `/api/device/query/${deviceId}/sync_status/`
  })
}

export function queryDevices(params) {
  const { page, count, query, status } = params
  return request({
    method: 'get',
    url: `/api/device/query/devices`,
    params: {
      page: page,
      count: count,
      query: query,
      status: status
    }
  })
}

export function deleteDevice(deviceId) {
  return request({
    method: 'delete',
    url: `/api/device/query/devices/${deviceId}/delete`
  })
}

export function sync(deviceId) {
  return request({
    method: 'get',
    url: `/api/device/query/devices/${deviceId}/sync`
  })
}

export function updateDeviceTransport(deviceId, streamMode) {
  return request({
    method: 'post',
    url: `/api/device/query/transport/${deviceId}/${streamMode}`
  })
}

export function setGuard(deviceId) {
  return request({
    method: 'get',
    url: `/api/device/control/guard/${deviceId}/SetGuard`
  })
}

export function resetGuard(deviceId) {
  return request({
    method: 'get',
    url: `/api/device/control/guard/${deviceId}/ResetGuard`
  })
}

export function subscribeCatalog(params) {
  const { id, cycle } = params
  return request({
    method: 'get',
    url: `/api/device/query/subscribe/catalog`,
    params: {
      id: id,
      cycle: cycle
    }
  })
}

export function subscribeMobilePosition(params) {
  const { id, cycle, interval } = params
  return request({
    method: 'get',
    url: `/api/device/query/subscribe/mobile-position`,
    params: {
      id: id,
      cycle: cycle,
      interval: interval
    }
  })
}

export function queryBasicParam(deviceId) {
  return request({
    method: 'get',
    url: `/api/device/config/query/${deviceId}/BasicParam`
  })
}

export function queryChannelOne(params) {
  const { deviceId, channelDeviceId } = params
  return request({
    method: 'get',
    url: '/api/device/query/channel/one',
    params: {
      deviceId: deviceId,
      channelDeviceId: channelDeviceId
    }
  })
}

export function queryChannels(deviceId, params) {
  const { page, count, query, online, channelType, catalogUnderDevice } = params
  return request({
    method: 'get',
    url: `/api/device/query/devices/${deviceId}/channels`,
    params: {
      page: page,
      count: count,
      query: query,
      online: online,
      channelType: channelType,
      catalogUnderDevice: catalogUnderDevice
    }
  })
}

export function deviceRecord(params) {
  const { deviceId, channelId, recordCmdStr } = params
  return request({
    method: 'get',
    url: `/api/device/control/record`,
    params: {
      deviceId: deviceId,
      channelId: channelId,
      recordCmdStr: recordCmdStr
    }
  })
}

export function querySubChannels(params, deviceId, parentChannelId) {
  const { page, count, query, online, channelType } = params
  return request({
    method: 'get',
    url: `/api/device/query/sub_channels/${deviceId}/${parentChannelId}/channels`,
    params: {
      page: page,
      count: count,
      query: query,
      online: online,
      channelType: channelType
    }
  })
}

export function queryChannelTree(params) {
  const { parentId, page, count } = params
  return request({
    method: 'get',
    url: `/api/device/query/tree/channel/${this.deviceId}`,
    params: {
      parentId: parentId,
      page: page,
      count: count
    }
  })
}

export function changeChannelAudio(params) {
  const { channelId, audio } = params
  return request({
    method: 'post',
    url: `/api/device/query/channel/audio`,
    params: {
      channelId: channelId,
      audio: audio
    }
  })
}

export function updateChannelStreamIdentification(params) {
  const { deviceDbId, streamIdentification } = params
  return request({
    method: 'post',
    url: `/api/device/query/channel/stream/identification/update/`,
    params: {
      deviceDbId: deviceDbId,
      streamIdentification: streamIdentification
    }
  })
}

export function update(data) {
  return request({
    method: 'post',
    url: `/api/device/query/device/update`,
    data: data
  })
}
export function add(data) {
  return request({
    method: 'post',
    url: `/api/device/query/device/add`,
    data: data
  })
}

export function queryDeviceOne(deviceId) {
  return request({
    method: 'get',
    url: `/api/device/query/devices/${deviceId}`
  })
}

export function queryDeviceTree(params, deviceId) {
  const { page, count, parentId, onlyCatalog } = params
  return request({
    method: 'get',
    url: `/api/device/query/tree/${deviceId}`,
    params: {
      page: page,
      count: count,
      parentId: parentId,
      onlyCatalog: onlyCatalog
    }
  })
}

