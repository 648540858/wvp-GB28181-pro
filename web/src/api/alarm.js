import request from '@/utils/request'

export function getAlarmList(params) {
  const { page, count, alarmType, beginTime, endTime } = params
  const query = new URLSearchParams()
  query.append('page', page)
  query.append('count', count)
  if (alarmType && alarmType.length > 0) {
    alarmType.forEach(t => query.append('alarmType', t))
  }
  if (beginTime) query.append('beginTime', beginTime)
  if (endTime) query.append('endTime', endTime)
  return request({
    method: 'get',
    url: `/api/alarm/list?${query.toString()}`
  })
}

export function deleteAlarms(ids) {
  return request({
    method: 'delete',
    url: '/api/alarm/delete',
    data: ids
  })
}
