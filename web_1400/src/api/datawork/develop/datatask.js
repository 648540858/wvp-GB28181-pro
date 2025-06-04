import request from '@/utils/request'

export function pageDataTask(params) {
  return request({
    url: '/api/dwn/data/compose/task/page',
    method: 'get',
    params: params
  })
}

export function getDataTask(id) {
  return request({
    url: '/api/dwn/data/compose/task/' + id,
    method: 'get'
  })
}

export function addDataTask(data) {
  return request({
    url: '/api/dwn/data/compose/task',
    method: 'post',
    data: data
  })
}

export function updateDataTask(data) {
  return request({
    url: '/api/dwn/data/compose/task',
    method: 'put',
    data: data
  })
}

export function updateDataTaskRunning(id, running) {
  return request({
    url: '/api/dwn/data/compose/task/running',
    method: 'put',
    params: {id: id, running: running}
  })
}

export function updateDataTaskDag(data) {
  return request({
    url: '/api/dwn/data/compose/task/dag',
    method: 'put',
    data: data
  })
}

export function delDataTasks(ids) {
  return request({
    url: '/api/dwn/data/compose/task/' + ids,
    method: 'delete'
  })
}

export function executeDateTask(id) {
  return request({
    url: '/api/dwn/data/compose/execute/' + id,
    method: 'get'
  })
}

export function getMaterialList() {
  return request({
    url: '/api/dwn/common/dag/material',
    method: 'get'
  })
}
