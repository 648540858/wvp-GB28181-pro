import request from '@/utils/request'

// 系统设置API

export function getProfileList() {
  return request({
    method: 'get',
    url: `/api/setting/profiles`
  })
}

// 查看配置文件内容
export function getProfileContent(profileId) {
  return request({
    method: 'get',
    url: `/api/setting/profile/content`,
    params: {
      profileId: profileId
    }
  })
}

// 切换配置文件(修改 spring.profiles.active, 需重启生效)
export function switchProfile(profileId) {
  return request({
    method: 'post',
    url: `/api/setting/profile/switch`,
    params: {
      profileId: profileId
    }
  })
}

// 重启服务(由服务端脚本完成)
export function restartServer() {
  return request({
    method: 'post',
    url: `/api/setting/restart`
  })
}

// 查询WEB服务配置(端口、HTTPS证书等)
export function getWebServerConfig() {
  return request({
    method: 'get',
    url: `/api/setting/web-server`
  })
}

// 保存WEB服务配置(需重启生效)
export function saveWebServerConfig(data) {
  return request({
    method: 'post',
    url: `/api/setting/web-server`,
    data
  })
}

// 查询国标服务配置
export function getSipServerConfig() {
  return request({
    method: 'get',
    url: `/api/setting/sip-server`
  })
}

// 保存国标服务配置(需重启生效)
export function saveSipServerConfig(data) {
  return request({
    method: 'post',
    url: `/api/setting/sip-server`,
    data
  })
}

// 查询部标服务配置
export function getJtServerConfig() {
  return request({
    method: 'get',
    url: `/api/setting/jt-server`
  })
}

// 保存部标服务配置(需重启生效)
export function saveJtServerConfig(data) {
  return request({
    method: 'post',
    url: `/api/setting/jt-server`,
    data
  })
}

// 查询平台策略配置
export function getUserSettingConfig() {
  return request({
    method: 'get',
    url: `/api/setting/user-setting`
  })
}

// 保存平台策略配置(无需重启的字段即时生效, 需要重启的字段由返回结果标识)
export function saveUserSettingConfig(data) {
  return request({
    method: 'post',
    url: `/api/setting/user-setting`,
    data
  })
}

// 查询报警订阅类型列表
export function getAlarmTypes() {
  return request({
    method: 'get',
    url: `/api/setting/alarm-types`
  })
}
