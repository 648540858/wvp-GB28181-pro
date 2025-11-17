import request from '@/utils/request'

// 分组API

export function update(data) {
  return request({
    method: 'post',
    url: '/api/group/update',
    data: data
  })
}
export function add(data) {
  return request({
    method: 'post',
    url: '/api/group/add',
    data: data
  })
}
export function getTreeList({ query, parent, hasChannel, page, count }) {
  return request({
    method: 'get',
    url: `/api/group/tree/list`,
    params: {
      query: query,
      parent: parent,
      hasChannel: hasChannel,
      page: page,
      count: count
    }
  })
}
export function deleteGroup(id) {
  return request({
    method: 'delete',
    url: `/api/group/delete`,
    params: {
      id: id
    }
  })
}
export function getPath(params) {
  const { deviceId, businessGroup } = params
  return request({
    method: 'get',
    url: `/api/group/path`,
    params: {
      deviceId: deviceId,
      businessGroup: businessGroup
    }
  })
}
export function queryTree(params) {
  const { page, count, query } = params
  return request({
    method: 'get',
    url: `/api/group/tree/query`,
    params: {
      query: query,
      page: page,
      count: count
    }
  })
}
