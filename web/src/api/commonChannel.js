import request from '@/utils/request'

// 通用通道API

export function update(data) {
  return request({
    method: 'post',
    url: '/api/common/channel/update',
    data: data
  })
}

export function add(data) {
  return request({
    method: 'post',
    url: '/api/common/channel/add',
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

export function queryOne(id) {
  return request({
    method: 'get',
    url: '/api/common/channel/one',
    params: {
      id: id
    }
  })
}

export function addDeviceToGroup(params) {
  const { parentId, businessGroup, deviceIds } = params
  return request({
    method: 'post',
    url: `/api/common/channel/group/device/add`,
    data: {
      parentId: parentId,
      businessGroup: businessGroup,
      deviceIds: deviceIds
    }
  })
}

export function addToGroup(params) {
  const { parentId, businessGroup, channelIds } = params
  return request({
    method: 'post',
    url: `/api/common/channel/group/add`,
    data: {
      parentId: parentId,
      businessGroup: businessGroup,
      channelIds: channelIds
    }
  })
}

export function deleteDeviceFromGroup(deviceIds) {
  return request({
    method: 'post',
    url: `/api/common/channel/group/device/delete`,
    data: {
      deviceIds: deviceIds
    }
  })
}

export function deleteFromGroup(channels) {
  return request({
    method: 'post',
    url: `/api/common/channel/group/delete`,
    data: {
      channelIds: channels
    }
  })
}

export function addDeviceToRegion(params) {
  const { civilCode, deviceIds } = params
  return request({
    method: 'post',
    url: `/api/common/channel/region/device/add`,
    data: {
      civilCode: civilCode,
      deviceIds: deviceIds
    }
  })
}
export function addToRegion(params) {
  const { civilCode, channelIds } = params
  return request({
    method: 'post',
    url: `/api/common/channel/region/add`,
    data: {
      civilCode: civilCode,
      channelIds: channelIds
    }
  })
}

export function deleteDeviceFromRegion(deviceIds) {
  return request({
    method: 'post',
    url: `/api/common/channel/region/device/delete`,
    data: {
      deviceIds: deviceIds
    }
  })
}
export function deleteFromRegion(channels) {
  return request({
    method: 'post',
    url: `/api/common/channel/region/delete`,
    data: {
      channelIds: channels
    }
  })
}

export function getCivilCodeList(params) {
  const { page, count, channelType, query, online, civilCode } = params
  return request({
    method: 'get',
    url: `/api/common/channel/civilcode/list`,
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

export function getParentList(params) {
  const { page, count, channelType, query, online, groupDeviceId } = params
  return request({
    method: 'get',
    url: `/api/common/channel/parent/list`,
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

export function getUnusualParentList(params) {
  const { page, count, channelType, query, online } = params
  return request({
    method: 'get',
    url: `/api/common/channel/parent/unusual/list`,
    params: {
      page: page,
      count: count,
      channelType: channelType,
      query: query,
      online: online
    }
  })
}

export function clearUnusualParentList(params) {
  const { all, channelIds } = params
  return request({
    method: 'post',
    url: `/api/common/channel/parent/unusual/clear`,
    data: {
      all: all,
      channelIds: channelIds
    }
  })
}

export function getUnusualCivilCodeList(params) {
  const { page, count, channelType, query, online } = params
  return request({
    method: 'get',
    url: `/api/common/channel/civilCode/unusual/list`,
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
    url: `/api/common/channel/civilCode/unusual/clear`,
    data: {
      all: all,
      channelIds: channelIds
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

export function playChannel(channelId) {
  return request({
    method: 'get',
    url: '/api/common/channel/play',
    params: {
      channelId: channelId
    }
  })
}
