import request from '@/utils/request'

// 云端录像API

export function getAll() {
  return request({
    method: 'get',
    url: '/api/role/all'
  })
}

