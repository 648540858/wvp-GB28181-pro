import request from '@/utils/request'

// 服务API

export function getOnlineMediaServerList() {
  return request({
    method: 'get',
    url: `/api/server/media_server/online/list`
  })
}

export function getMediaServerList() {
  return request({
    method: 'get',
    url: `/api/server/media_server/list`
  })
}

export function getMediaServer(id) {
  return request({
    method: 'get',
    url: `/api/server/media_server/one/` + id
  })
}

export function checkMediaServer(params) {
  const { ip, httpPort, secret, type } = params
  return request({
    method: 'get',
    url: `/api/server/media_server/check`,
    params: {
      ip: ip,
      port: httpPort,
      secret: secret,
      type: type
    }
  })
}

export function checkMediaServerRecord(params) {
  const { ip, port } = params
  return request({
    method: 'get',
    url: `/api/server/media_server/record/check`,
    params: {
      ip: ip,
      port: port
    }
  })
}

export function saveMediaServer(formData) {
  return request({
    method: 'post',
    url: `/api/server/media_server/save`,
    data: formData
  })
}

export function deleteMediaServer(id) {
  return request({
    method: 'delete',
    url: `/api/server/media_server/delete`,
    params: {
      id: id
    }
  })
}

export function getSystemConfig() {
  return request({
    method: 'get',
    url: `/api/server/system/configInfo`
  })
}

export function getMediaInfo(params) {
  const { app, stream, mediaServerId } = params
  return request({
    method: 'get',
    url: `/api/server/media_server/media_info`,
    params: {
      app: app,
      stream: stream,
      mediaServerId: mediaServerId
    }
  })
}

export function getSystemInfo() {
  return request({
    method: 'get',
    url: `/api/server/system/info`
  })
}

export function getMediaServerLoad() {
  return request({
    method: 'get',
    url: `/api/server/media_server/load`
  })
}

export function getResourceInfo() {
  return request({
    method: 'get',
    url: `/api/server/resource/info`
  })
}

export function info() {
  return request({
    method: 'get',
    url: `/api/server/info`
  })
}

