import request from '@/utils/request'

export function pageScheduleLog(params) {
  return request({
    url: '/api/dwn/data/schedule/log/page',
    method: 'get',
    params: params
  })
}

export function delScheduleLogs(ids) {
    return request({
      url: '/api/dwn/data/schedule/log/' + ids,
      method: 'delete'
    })
  }