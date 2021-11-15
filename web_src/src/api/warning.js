import request from '@/utils/request'

/**
 *
 * 获取报警事件列表
 */
export function getWarningList(parameter){
  return request({
    url: '/api/alarm/all',
    method: 'get',
    params: parameter
  })
}

/**
 *
 * 删除报警信息
 */
export function deleteWarning(parameter){
  return request({
    url: '/api/alarm/delete',
    method: 'delete',
    params: parameter
  })
}