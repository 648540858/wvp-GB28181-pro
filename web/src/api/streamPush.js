import request from '@/utils/request'

// 推流列表API

export function saveToGb(data) {
  return request({
    method: 'post',
    url: `/api/push/save_to_gb`,
    data: data
  })
}

export function add(data) {
  return request({
    method: 'post',
    url: `/api/push/add`,
    data: data
  })
}

export function update(data) {
  return request({
    method: 'post',
    url: '/api/push/update',
    data: data
  })
}

export function queryList(params) {
  const { page, count, query, pushing, mediaServerId } = params
  return request({
    method: 'get',
    url: `/api/push/list`,
    params: {
      page: page,
      count: count,
      query: query,
      pushing: pushing,
      mediaServerId: mediaServerId
    }
  })
}

export function play(id) {
  return request({
    method: 'get',
    url: '/api/push/start',
    params: {
      id: id
    }
  })
}

export function remove(id) {
  return request({
    method: 'post',
    url: '/api/push/remove',
    params: {
      id: id
    }
  })
}

export function removeFormGb(data) {
  return request({
    method: 'delete',
    url: '/api/push/remove_form_gb',
    data: data
  })
}

export function batchRemove(ids) {
  return request({
    method: 'delete',
    url: '/api/push/batchRemove',
    data: {
      ids: ids
    }
  })
}
