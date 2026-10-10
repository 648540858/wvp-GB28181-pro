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
