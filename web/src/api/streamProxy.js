import request from '@/utils/request'

// 拉流代理API

export function queryFfmpegCmdList(mediaServerId) {
  return request({
    method: 'get',
    url: `/api/proxy/ffmpeg_cmd/list`,
    params: {
      mediaServerId: mediaServerId
    }
  })
}

export function save(data) {
  return request({
    method: 'post',
    url: `/api/proxy/save`,
    data: data

  })
}

export function update(data) {
  return request({
    method: 'post',
    url: `/api/proxy/update`,
    data: data
  })
}

export function add(data) {
  return request({
    method: 'post',
    url: `/api/proxy/add`,
    data: data
  })
}

export function queryList(params) {
  const { page, count, query, pulling, mediaServerId } = params
  return request({
    method: 'get',
    url: `/api/proxy/list`,
    params: {
      page: page,
      count: count,
      query: query,
      pulling: pulling,
      mediaServerId: mediaServerId
    }
  })
}

export function play(id) {
  return request({
    method: 'get',
    url: `/api/proxy/start`,
    params: {
      id: id
    }
  })
}

export function stopPlay(id) {
  return request({
    method: 'get',
    url: `/api/proxy/stop`,
    params: {
      id: id
    }
  })
}

export function remove(id) {
  return request({
    method: 'delete',
    url: '/api/proxy/delete',
    params: {
      id: id
    }

  })
}

