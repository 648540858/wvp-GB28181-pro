import request from '@/utils/request'

/**
 *
 * 获取各epoll(或select)线程负载以及延时
 */
export function getThreadsLoad(parameter) {
  return request({
    url: '/zlm/' + parameter.mediaServerId + '/index/api/getThreadsLoad',
    method: 'get'
  })
}

/**
 *
 * 获取在线的ZLM服务器列表
 */
export function getOnlineMediaServerList() {
  return request({
    url: '/api/server/mediaServer/online/list',
    method: 'get'
  })
}

/**
 *
 * 获取ZLM服务器session会话
 */
export function getAllSession(parameter) {
  return request({
    url: '/zlm/' + parameter.mediaServerId +'/index/api/getAllSession',
    method: 'get'
  })
}

/**
 *
 * 获取ZLM配置信息
 */
export function getServerConfig(parameter){
  return request({
    url: '/zlm/' + parameter.mediaServerId +'/index/api/getServerConfig',
    method: 'get'
  })
}

/**
 *
 * 获取信息服务器WVP配置信息
 */
export function getWVPServerConfig(){
  return request({
    url: '/api/server/config',
    method: 'get'
  })
}

/**
 *
 * 重启媒体服务器
 */
export function restartServer(parameter){
  return request({
    url: '/zlm/'+ parameter.mediaServerId +'/index/api/restartServer',
    method: 'get'
  })
}

/**
 *
 * 删除session会话
 */
export function deleteSession(parameter){
  return request({
    url: '/zlm/' + parameter.mediaServerId +'/index/api/kick_session&id=' + parameter.id,
    method: 'get'
  })
}