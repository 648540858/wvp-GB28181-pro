import request from '@/utils/request'

/**
 * 获取设备tree
 */
export function getVideoTree(parameter) {
  return request({
    url: '/api/square/video/tree',
    method: 'get',
    params: parameter
  })
}
