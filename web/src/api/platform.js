import request from '@/utils/request'

export function update(data) {
  return request({
    method: 'post',
    url: '/api/platform/update',
    data: data
  })
}

export function add(data) {
  return request({
    method: 'post',
    url: '/api/platform/add',
    data: data
  })
}

export function exit(deviceGbId) {
  return request({
    method: 'get',
    url: `/api/platform/exit/${deviceGbId}`
  })
}

export function remove(id) {
  return request({
    method: 'delete',
    url: `/api/platform/delete/`,
    params: {
      id: id
    }
  })
}

export function pushChannel(id) {
  return request({
    method: 'get',
    url: `/api/platform/channel/push`,
    params: {
      id: id
    }
  })
}

export function getServerConfig() {
  return request({
    method: 'get',
    url: `/api/platform/server_config`
  })
}

export function query(params) {
  const { count, page, query } = params
  return request({
    method: 'get',
    url: `/api/platform/query`,
    params: {
      count: count,
      page: page,
      query: query
    }

  })
}

export function getChannelList(params) {
  const { page, count, query, online, channelType, platformId, hasShare } = params
  return request({
    method: 'get',
    url: `/api/platform/channel/list`,
    params: {
      page: page,
      count: count,
      query: query,
      online: online,
      channelType: channelType,
      platformId: platformId,
      hasShare: hasShare
    }
  })
}

export function addChannel(params) {
  const { platformId, channelIds, all } = params
  return request({
    method: 'post',
    url: `/api/platform/channel/add`,
    data: {
      platformId: platformId,
      channelIds: channelIds,
      all: all
    }

  })
}

export function addChannelByDevice(params) {
  const { platformId, deviceIds } = params
  return request({
    method: 'post',
    url: `/api/platform/channel/device/add`,
    data: {
      platformId: platformId,
      deviceIds: deviceIds
    }
  })
}

export function removeChannelByDevice(params) {
  const { platformId, deviceIds } = params
  return request({
    method: 'post',
    url: `/api/platform/channel/device/remove`,
    data: {
      platformId: platformId,
      deviceIds: deviceIds
    }
  })
}

export function removeChannel(params) {
  const { platformId, channelIds, all } = params
  return request({
    method: 'delete',
    url: `/api/platform/channel/remove`,
    data: {
      platformId: platformId,
      channelIds: channelIds,
      all: all
    }
  })
}

export function updateCustomChannel(data) {
  return request({
    method: 'post',
    url: `/api/platform/channel/custom/update`,
    data: data
  })
}

