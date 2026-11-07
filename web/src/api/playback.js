import request from '@/utils/request'

// 回放流播放API

/** 默认复用同区间回放，仅在调用方指定身份时请求设备提供独立会话。 */
export function play([deviceId, channelId, startTime, endTime, sessionId]) {
  return request({
    method: 'get',
    url: '/api/playback/start/' + deviceId + '/' + channelId,
    params: { startTime, endTime, ...(sessionId !== undefined && sessionId !== null ? { sessionId } : {}) }
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
