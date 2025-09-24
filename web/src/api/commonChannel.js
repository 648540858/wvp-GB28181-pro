import request from '@/utils/request'

// 通用通道API

export function queryOne(id) {
  return request({
    method: 'get',
    url: '/api/common/channel/one',
    params: {
      id: id
    }
  })
}

export function getIndustryList() {
  return request({
    method: 'get',
    url: '/api/common/channel/industry/list'
  })
}

export function getTypeList() {
  return request({
    method: 'get',
    url: '/api/common/channel/type/list'
  })
}

export function getNetworkIdentificationList() {
  return request({
    method: 'get',
    url: '/api/common/channel/network/identification/list'
  })
}

export function update(data) {
  return request({
    method: 'post',
    url: '/api/common/channel/update',
    data: data
  })
}

export function reset(id) {
  return request({
    method: 'post',
    url: '/api/common/channel/reset',
    params: {
      id: id
    }
  })
}

export function add(data) {
  return request({
    method: 'post',
    url: '/api/common/channel/add',
    data: data
  })
}

export function getList(params) {
  const { page, count, query, online, hasRecordPlan, channelType } = params
  return request({
    method: 'get',
    url: '/api/common/channel/list',
    params: {
      page: page,
      count: count,
      channelType: channelType,
      query: query,
      online: online,
      hasRecordPlan: hasRecordPlan
    }
  })
}

export function getCivilCodeList(params) {
  const { page, count, channelType, query, online, civilCode } = params
  return request({
    method: 'get',
    url: '/api/common/channel/civilcode/list',
    params: {
      page: page,
      count: count,
      channelType: channelType,
      query: query,
      online: online,
      civilCode: civilCode
    }
  })
}

export function getUnusualCivilCodeList(params) {
  const { page, count, channelType, query, online } = params
  return request({
    method: 'get',
    url: '/api/common/channel/civilCode/unusual/list',
    params: {
      page: page,
      count: count,
      channelType: channelType,
      query: query,
      online: online
    }
  })
}

export function getUnusualParentList(params) {
  const { page, count, channelType, query, online } = params
  return request({
    method: 'get',
    url: '/api/common/channel/parent/unusual/list',
    params: {
      page: page,
      count: count,
      channelType: channelType,
      query: query,
      online: online
    }
  })
}

export function clearUnusualCivilCodeList(params) {
  const { all, channelIds } = params
  return request({
    method: 'post',
    url: '/api/common/channel/civilCode/unusual/clear',
    data: {
      all: all,
      channelIds: channelIds
    }
  })
}

export function clearUnusualParentList(params) {
  const { all, channelIds } = params
  return request({
    method: 'post',
    url: '/api/common/channel/parent/unusual/clear',
    data: {
      all: all,
      channelIds: channelIds
    }
  })
}

export function getParentList(params) {
  const { page, count, channelType, query, online, groupDeviceId } = params
  return request({
    method: 'get',
    url: '/api/common/channel/parent/list',
    params: {
      page: page,
      count: count,
      channelType: channelType,
      query: query,
      online: online,
      groupDeviceId: groupDeviceId
    }
  })
}

export function addToRegion(params) {
  const { civilCode, channelIds } = params
  return request({
    method: 'post',
    url: '/api/common/channel/region/add',
    data: {
      civilCode: civilCode,
      channelIds: channelIds
    }
  })
}

export function deleteFromRegion(channels) {
  return request({
    method: 'post',
    url: '/api/common/channel/region/delete',
    data: {
      channelIds: channels
    }
  })
}

export function addDeviceToRegion(params) {
  const { civilCode, deviceIds } = params
  return request({
    method: 'post',
    url: '/api/common/channel/region/device/add',
    data: {
      civilCode: civilCode,
      deviceIds: deviceIds
    }
  })
}

export function deleteDeviceFromRegion(deviceIds) {
  return request({
    method: 'post',
    url: '/api/common/channel/region/device/delete',
    data: {
      deviceIds: deviceIds
    }
  })
}

export function addToGroup(params) {
  const { parentId, businessGroup, channelIds } = params
  return request({
    method: 'post',
    url: '/api/common/channel/group/add',
    data: {
      parentId: parentId,
      businessGroup: businessGroup,
      channelIds: channelIds
    }
  })
}

export function deleteFromGroup(channels) {
  return request({
    method: 'post',
    url: '/api/common/channel/group/delete',
    data: {
      channelIds: channels
    }
  })
}

export function addDeviceToGroup(params) {
  const { parentId, businessGroup, deviceIds } = params
  return request({
    method: 'post',
    url: '/api/common/channel/group/device/add',
    data: {
      parentId: parentId,
      businessGroup: businessGroup,
      deviceIds: deviceIds
    }
  })
}

export function deleteDeviceFromGroup(deviceIds) {
  return request({
    method: 'post',
    url: '/api/common/channel/group/device/delete',
    data: {
      deviceIds: deviceIds
    }
  })
}

export function playChannel(channelId) {
  return request({
    method: 'get',
    url: '/api/common/channel/play',
    params: {
      channelId: channelId
    }
  })
}
export function stopPlayChannel(channelId) {
  return request({
    method: 'get',
    url: '/api/common/channel/play/stop',
    params: {
      channelId: channelId
    }
  })
}


// 前端控制

export function setSpeedForScan({ channelId, scanId, speed }) {
  return request({
    method: 'get',
    url: '/api/common/channel/front-end/scan/set/speed',
    params: {
      channelId: channelId,
      scanId: scanId,
      speed: speed
    }
  })
}

export function setLeftForScan({ channelId, scanId }) {
  return request({
    method: 'get',
    url: '/api/common/channel/front-end/scan/set/left',
    params: {
      channelId: channelId,
      scanId: scanId
    }
  })
}

export function setRightForScan({ channelId, scanId }) {
  return request({
    method: 'get',
    url: '/api/common/channel/front-end/scan/set/right',
    params: {
      channelId: channelId,
      scanId: scanId
    }

  })
}

export function startScan({ channelId, scanId }) {
  return request({
    method: 'get',
    url: '/api/common/channel/front-end/scan/start',
    params: {
      channelId: channelId,
      scanId: scanId
    }
  })
}

export function stopScan({ channelId, scanId }) {
  return request({
    method: 'get',
    url: '/api/common/channel/front-end/scan/stop',
    params: {
      channelId: channelId,
      scanId: scanId
    }

  })
}

export function queryPreset(channelId) {
  return request({
    method: 'get',
    url: '/api/common/channel/front-end/preset/query',
    params: {
      channelId: channelId
    }
  })
}

export function addPointForCruise({ channelId, tourId, presetId }) {
  return request({
    method: 'get',
    url: '/api/common/channel/front-end/tour/point/add',
    params: {
      channelId: channelId,
      tourId: tourId,
      presetId: presetId
    }
  })
}

export function deletePointForCruise({ channelId, tourId, presetId }) {
  return request({
    method: 'get',
    url: '/api/common/channel/front-end/tour/point/delete',
    params: {
      channelId: channelId,
      tourId: tourId,
      presetId: presetId
    }
  })
}

export function setCruiseSpeed({ channelId, tourId, presetId , speed }) {
  return request({
    method: 'get',
    url: '/api/common/channel/front-end/tour/speed',
    params: {
      channelId: channelId,
      tourId: tourId,
      presetId: presetId,
      speed: speed
    }
  })
}

export function setCruiseTime({ channelId, tourId, presetId, time }) {
  return request({
    method: 'get',
    url: '/api/common/channel/front-end/tour/time',
    params: {
      channelId: channelId,
      tourId: tourId,
      presetId: presetId,
      time: time
    }
  })
}

export function startCruise({ channelId, tourId }) {
  return request({
    method: 'get',
    url: '/api/common/channel/front-end/tour/start',
    params: {
      channelId: channelId,
      tourId: tourId
    }
  })
}

export function stopCruise({ channelId, tourId }) {
  return request({
    method: 'get',
    url: '/api/common/channel/front-end/tour/stop',
    params: {
      channelId: channelId,
      tourId: tourId
    }
  })
}

export function addPreset({ channelId, presetId, presetName }) {
  return request({
    method: 'get',
    url: '/api/common/channel/front-end/preset/add',
    params: {
      channelId: channelId,
      presetId: presetId,
      presetName: presetName
    }
  })
}

export function callPreset({ channelId, presetId }) {
  return request({
    method: 'get',
    url: '/api/common/channel/front-end/preset/call',
    params: {
      channelId: channelId,
      presetId: presetId
    }
  })
}

export function deletePreset({ channelId, presetId }) {
  return request({
    method: 'get',
    url: '/api/common/channel/front-end/preset/delete',
    params: {
      channelId: channelId,
      presetId: presetId
    }
  })
}

/**
 * command: on 开启， off 关闭
 */
export function auxiliary({ channelId, command, auxiliaryId }) {
  return request({
    method: 'get',
    url: '/api/common/channel/front-end/auxiliary',
    params: {
      channelId: channelId,
      command: command,
      auxiliaryId: auxiliaryId
    }
  })
}
/**
 * command: on 开启， off 关闭
 */
export function wiper({ channelId, command }) {
  return request({
    method: 'get',
    url: '/api/common/channel/front-end/wiper',
    params: {
      channelId: channelId,
      command: command
    }
  })
}

export function ptz({ channelId, command, panSpeed, tiltSpeed, zoomSpeed }) {
  return request({
    method: 'get',
    url: '/api/common/channel/front-end/ptz',
    params: {
      channelId: channelId,
      command: command,
      panSpeed: panSpeed,
      tiltSpeed: tiltSpeed,
      zoomSpeed: zoomSpeed
    }
  })
}

export function iris({ channelId, command, speed }) {
  return request({
    method: 'get',
    url: '/api/common/channel/front-end/fi/iris',
    params: {
      channelId: channelId,
      command: command,
      speed: speed
    }
  })
}

export function focus({ channelId, command, speed }) {
  return request({
    method: 'get',
    url: '/api/common/channel/front-end/fi/focus',
    params: {
      channelId: channelId,
      command: command,
      speed: speed
    }
  })
}
export function queryRecord({ channelId, startTime, endTime }) {
  return request({
    method: 'get',
    url: '/api/common/channel/playback/query',
    params: {
      channelId: channelId,
      startTime: startTime,
      endTime: endTime
    }
  })
}
export function playback({ channelId, startTime, endTime }) {
  return request({
    method: 'get',
    url: '/api/common/channel/playback',
    params: {
      channelId: channelId,
      startTime: startTime,
      endTime: endTime
    }
  })
}
export function stopPlayback({ channelId, stream }) {
  return request({
    method: 'get',
    url: '/api/common/channel/playback/stop',
    params: {
      channelId: channelId,
      stream: stream
    }
  })
}
export function pausePlayback({ channelId, stream}) {
  return request({
    method: 'get',
    url: '/api/common/channel/playback/pause',
    params: {
      channelId: channelId,
      stream: stream
    }
  })
}
export function resumePlayback({ channelId, stream}) {
  return request({
    method: 'get',
    url: '/api/common/channel/playback/resume',
    params: {
      channelId: channelId,
      stream: stream
    }
  })
}
export function seekPlayback({ channelId, stream, seekTime}) {
  return request({
    method: 'get',
    url: '/api/common/channel/playback/seek',
    params: {
      channelId: channelId,
      stream: stream,
      seekTime: seekTime
    }
  })
}
export function speedPlayback({ channelId, stream, speed}) {
  return request({
    method: 'get',
    url: '/api/common/channel/playback/speed',
    params: {
      channelId: channelId,
      stream: stream,
      speed: speed
    }
  })
}
export function getAllForMap({ query, online, hasRecordPlan, channelType }) {
  return request({
    method: 'get',
    url: '/api/common/channel/map/list',
    params: {
      query: query,
      online: online,
      hasRecordPlan: hasRecordPlan,
      channelType: channelType
    }
  })
}
