import request from "@/utils/request";

/**
 *
 * 获取流代理列表
 */
export function getStreamProxyList(parameter){
  return request({
    url: '/api/proxy/list',
    method: 'get',
    params: parameter
  })
}


/**
 *
 * 获取推流列表
 */
export function getStreamPushList(parameter){
  return request({
    url: '/api/push/list',
    method: 'get',
    params: parameter
  })
}

/**
 *
 * 查询级联平台
 */
export function queryPlatforms(){
  return request({
    url: '/api/platform/query/10000/0',
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
 * 获取ffmpeg.cmd模板
 */
export function getFFmpegCMDs(parameter){
  return request({
    url: '/api/proxy/ffmpeg_cmd/list',
    method: 'get',
    params: parameter
  })
}

/**
 *
 * 添加代理
 */
export function saveStreamProxy(parameter){
  return request({
    url: '/api/proxy/save',
    method: 'post',
    data: parameter
  })
}

/**
 *
 * 推流添加到国标
 */
export function addTOGBPush(parameter){
  return request({
    url: '/api/push/save_to_gb',
    method: 'post',
    data: parameter
  })
}
/**
 *
 * 推流移出到国标
 */
export function removeTOGBPush(parameter){
  return request({
    url: '/api/push/remove_form_gb',
    method: 'delete',
    data: parameter
  })
}

/**
 *
 * 根据应用名和流id获取播放地址
 */
export function getStreamInfoByAppAndStream(parameter){
  return request({
    url: '/api/media/stream_info_by_app_and_stream',
    method: 'get',
    params: parameter
  })
}

/**
 *
 * 停用代理
 */
export function stop(parameter){
  return request({
    url: '/api/proxy/stop',
    method: 'get',
    params: parameter
  })
}

/**
 *
 * 启用代理
 */
export function startProxy(parameter){
  return request({
    url: '/api/proxy/start',
    method: 'get',
    params: parameter
  })
}

/**
 *
 * 移除代理
 */
export function deleteProxy(parameter){
  return request({
    url: '/api/proxy/del',
    method: 'delete',
    params: parameter
  })
}

