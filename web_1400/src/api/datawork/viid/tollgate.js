import request from '@/utils/request'

export function pageTollgate(data) {
  return request({
    url: '/api/viid/tollgate/page',
    method: 'get',
    params: data
  })
}

export function getTollgateOptions(params) {
  return request({
    url: '/api/viid/tollgate/page',
    method: 'get',
    headers: { showLoading: false },
    params: params
  })
}

export function getTollgate(id) {
  return request({
    url: '/api/viid/tollgate/' + id,
    method: 'get',
  })
}

export function addTollgate(data) {
  return request({
    url: '/api/viid/tollgate',
    method: 'post',
    data: data
  })
}

export function updateTollgate(data) {
  return request({
    url: '/api/viid/tollgate',
    method: 'put',
    data: data
  })
}

export function delTollgates(ids) {
  return request({
    url: '/api/viid/tollgate/device/' + ids,
    method: 'delete'
  })
}