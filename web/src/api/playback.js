import request from '@/utils/request'

// 回放流播放API

export function play([deviceId, channelId, startTime, endTime]) {
  return request({
    method: 'get',
    url: '/api/playback/start/' + deviceId + '/' + channelId + '?startTime=' + startTime + '&endTime=' + endTime
  })
}
export function resume(streamId) {
  return request({
    method: 'get',
    url: '/api/playback/resume/' + streamId
  })
}
export function pause(streamId) {
  return request({
    method: 'get',
    url: '/api/playback/pause/' + streamId
  })
}
export function setSpeed([streamId, speed]) {
  return request({
    method: 'get',
    url: `/api/playback/speed/${streamId}/${speed}`
  })
}
export function stop(deviceId, channelId, streamId) {
  return request({
    method: 'get',
    url: '/api/playback/stop/' + deviceId + '/' + channelId + '/' + streamId
  })
}
