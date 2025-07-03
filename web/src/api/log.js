import request from '@/utils/request'

export function queryList(params) {
  const { query, startTime, endTime } = params
  return request({
    method: 'get',
    url: `/api/log/list`,
    params: {
      query: query,
      startTime: startTime,
      endTime: endTime
    }
  })
}

