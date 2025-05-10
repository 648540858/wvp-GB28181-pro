import request from '@/utils/request'

// 行政区划API

export function getTreeList(params) {
  const {query, parent, hasChannel} = params
  return request({
    method: 'get',
    url: `/api/region/tree/list`,
    params: {
      query: query,
      parent: parent,
      hasChannel: hasChannel
    }
  })
}

export function deleteRegion(id) {
  return request({
    method: "delete",
    url: `/api/region/delete`,
    params: {
      id: id,
    }
  })
}

export function description(civilCode) {
  return request({
    method: 'get',
    url: `/api/region/description`,
    params: {
      civilCode: civilCode,
    }
  })
}

export function addByCivilCode(civilCode) {
  return request({
    method: 'get',
    url: `/api/region/addByCivilCode`,
    params: {
      civilCode: civilCode,
    }
  })
}

export function queryChildListInBase(parent) {
  return request({
    method: 'get',
    url: "/api/region/base/child/list",
    params: {
      parent: parent,
    }
  })
}

export function update(data) {
  return request({
    method: 'post',
    url: "/api/region/update",
    data: data

  })
}

export function add(data) {
  return request({
    method: 'post',
    url: "/api/region/add",
    data: data
  })
}

export function queryPath(deviceId) {
  return request({
    method: 'get',
    url: `/api/region/path`,
    params: {
      deviceId: deviceId,
    }
  })
}

