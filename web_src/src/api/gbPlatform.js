import request from "@/utils/request";

/**
 *
 *分页查询级联平台
 */
export function getPlatformList(parameter) {
  return request({
    url: '/api/platform/query/' + parameter.pageSize + '/' + parameter.pageNo,
    method: 'get',
    params: parameter
  })
}

/**
 *
 * 获取国标服务的配置
 */
export function getPlatformServerConf() {
  return request({
    url: '/api/platform/server_config',
    method: 'get'
  })
}

/**
 *
 * 保存上级平台信息
 */
export function savePlatform(parameter) {
  return request({
    url: '/api/platform/save',
    method: 'post',
    data: parameter
  })
}

/**
 *
 * 查询上级平台是否存在
 */
export function exitPlatform(parameter) {
  return request({
    url: '/api/platform/exit/' + parameter.deviceGbId,
    method: 'get'
  })
}

/**
 *
 *删除上级平台
 */
export function deletePlatformCommit(parameter) {
  return request({
    url: '/api/platform/delete/' + parameter.serverGBId,
    method: 'delete'
  })
}

/**
 *
 * 分页查询级联平台的所有所有通道
 */
export function getChannelList(parameter) {
  return request({
    url: '/api/platform/channel_list',
    method: 'get',
    params: parameter
  })
}

/**
 *
 * 向上级平台添加国标通道
 */
export function updateChannelForGB(parameter) {
  return request({
    url: '/api/platform/update_channel_for_gb',
    method: 'post',
    data: parameter
  })
}

/**
 *
 * 从上级平台移除国标通道
 */
export function delChannelForGB(parameter) {
  return request({
    url: '/api/platform/del_channel_for_gb',
    method: 'delete',
    data: parameter
  })
}

/**
 *
 * 查询国标通道
 */
export function queryGbChannel(parameter){
  return request({
    url: '/api/gbStream/list',
    method: 'get',
    params: parameter
  })
}