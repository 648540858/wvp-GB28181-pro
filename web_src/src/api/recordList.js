import request from '@/utils/request'

/**
 *
 * 获取在线的 Media Server
 */
export function getOnlineMediaServerList(parameter) {
  return request({
    url: '/api/server/mediaServer/online/list',
    method: 'get',
    params: parameter
  })
}

/**
 *
 * 根据 MediaServer Id获取录像列表
 */
export function getRecordList(parameter) {
  return request({
    url: '/record_proxy/' + parameter.mediaServerId + '/api/record/list',
    method: 'get',
    params: parameter
  })
}

/**
 *
 * 构造录像记录返回值
 */
export function resetRecordList(parameter) {
  return request({
    url: '/api/record/resetRecords',
    method: 'post',
    data: parameter
  })
}

/**
 *
 * 获取录像文件列表
 */
export function recordDateList(parameter) {
  return request({
    url: '/record_proxy/' + parameter.mediaServerId + '/api/record/date/list',
    method: 'get',
    params: parameter
  })
}

/**
 *
 * 获取录像文件列表
 */
export function queryRecordDetails(parameter){
  return request({
    url: '/record_proxy/' + parameter.mediaServerId + '/api/record/file/list',
    method: 'get',
    params: parameter
  })
}


/**
 *
 * 查询视频裁剪合并任务列表
 */
export function getDownLoadTaskList(parameter) {
  return request({
    url: '/record_proxy/'+parameter.mediaServerId+'/api/record/file/download/task/list',
    method: 'get',
    params: parameter
  })
}

/**
 *
 * 查询视频裁剪合并任务列表
 */
export function addDownLoadTask(parameter) {
  return request({
    url: '/record_proxy/'+parameter.mediaServerId+'/api/record/file/download/task/add',
    method: 'get',
    params: parameter
  })
}

/**
 *
 * 删除裁剪合并后得视频
 */
export function deleteDownLoadTask(parameter) {
  return request({
    url: '/record_proxy/'+parameter.mediaServerId+'/api/record/file/download/task/delete',
    method: 'get',
    params: parameter
  })
}
