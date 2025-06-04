import request from '@/utils/request'

export function getServerOptions(params) {
  return request({
    url: '/api/viid/server/options',
    method: 'get',
    params: params,
    headers: { showLoading: false },
  })
}

export function getSubscribeDetailOptions() {
  return request({
    url: '/api/viid/subscribe/detail/options',
    method: 'get',
    headers: { showLoading: false },
  })
}

export function getTollgateOptions() {
  return request({
    url: '/api/viid/tollgate/device/options',
    method: 'get',
    headers: { showLoading: false },
  })
}
