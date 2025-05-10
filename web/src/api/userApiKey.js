import request from '@/utils/request'

export function remark(params) {
  const { id, remark } = params
  return request({
    method: 'post',
    url: '/api/userApiKey/remark',
    params: {
      id: id,
      remark: remark
    }
  })
}

export function queryList(params) {
  const { page, count } = params
  return request({
    method: 'get',
    url: `/api/userApiKey/userApiKeys`,
    params: {
      page: page,
      count: count
    }
  })
}

export function enable(id) {
  return request({
    method: 'post',
    url: `/api/userApiKey/enable?id=${id}`

  })
}

export function disable(id) {
  return request({
    method: 'post',
    url: `/api/userApiKey/disable?id=${id}`
  })
}

export function reset(id) {
  return request({
    method: 'post',
    url: `/api/userApiKey/reset?id=${id}`
  })
}

export function remove(id) {
  return request({
    method: 'delete',
    url: `/api/userApiKey/delete?id=${id}`
  })
}

export function add(params) {
  const { userId, app, enable, expiresAt, remark } = params
  return request({
    method: 'post',
    url: '/api/userApiKey/add',
    params: {
      userId: userId,
      app: app,
      enable: enable,
      expiresAt: expiresAt,
      remark: remark
    }
  })
}
