import request from '@/utils/request'

// 实时流播放API

export function play(deviceId, channelId) {
  return request({
    method: 'get',
    url: '/api/play/start/' + deviceId + '/' + channelId
  })
}
export function stop(deviceId, channelId) {
  return request({
    method: 'get',
    url: '/api/play/stop/' + deviceId + "/" + channelId,
  })
}
export function broadcastStart(deviceId, channelId, broadcastMode) {
  return request({
    method: 'get',
    url: '/api/play/broadcast/' + deviceId + '/' + channelId + "?timeout=30&broadcastMode=" + broadcastMode
  })
}
export function broadcastStop(deviceId, channelId, ) {
  return request({
    method: 'get',
    url: '/api/play/broadcast/stop/' + deviceId + '/' + channelId
  })
}
