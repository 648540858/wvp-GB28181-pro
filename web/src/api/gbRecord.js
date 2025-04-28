import request from '@/utils/request'

export function query([deviceId, channelId, startTime, endTime]) {
  return request({
    method: 'get',
    url: '/api/gb_record/query/' + deviceId + '/' + channelId + '?startTime=' + startTime + '&endTime=' + endTime

  })
}

export function startDownLoad([deviceId, channelId, startTime, endTime, downloadSpeed]) {
  return request({
    url: '/api/gb_record/download/start/' + deviceId + '/' + channelId + '?startTime=' + startTime + '&endTime=' +
      endTime + '&downloadSpeed=' + downloadSpeed

  })
}

export function stopDownLoad(deviceId, channelId, streamId) {
  return request({
    method: 'get',
    url: '/api/gb_record/download/stop/' + deviceId + '/' + channelId + '/' + streamId

  })
}

export function queryDownloadProgress([deviceId, channelId, streamId]) {
  return request({
    method: 'get',
    url: `/api/gb_record/download/progress/${deviceId}/${channelId}/${streamId}`
  })
}

