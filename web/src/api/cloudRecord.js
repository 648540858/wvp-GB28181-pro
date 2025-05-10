import request from '@/utils/request'

// 云端录像API

export function getPlayPath(id) {
  return request({
    method: 'get',
    url: `/api/cloud/record/play/path`,
    params: {
      recordId: id
    }
  })
}

export function queryListByData(params) {
  const { app, stream, year, month, mediaServerId } = params
  return request({
    method: 'get',
    url: `/api/cloud/record/date/list`,
    params: {
      app: app,
      stream: stream,
      year: year,
      month: month,
      mediaServerId: mediaServerId
    }
  })
}

export function loadRecord(params) {
  const { app, stream, date } = params
  return request({
    method: 'get',
    url: `/api/cloud/record/loadRecord`,
    params: {
      app: app,
      stream: stream,
      date: date
    }
  })
}

export function seek(params) {
  const { mediaServerId, app, stream, seek, schema } = params
  return request({
    method: 'get',
    url: `/api/cloud/record/seek`,
    params: {
      mediaServerId: mediaServerId,
      app: app,
      stream: stream,
      seek: seek,
      schema: schema
    }
  })
}

export function speed(params) {
  const { mediaServerId, app, stream, speed, schema } = params
  return request({
    method: 'get',
    url: `/api/cloud/record/speed`,
    params: {
      mediaServerId: mediaServerId,
      app: app,
      stream: stream,
      speed: speed,
      schema: schema
    }
  })
}

export function addTask(params) {
  const { app, stream, mediaServerId, startTime, endTime } = params
  return request({
    method: 'get',
    url: `/api/cloud/record/task/add`,
    params: {
      app: app,
      stream: stream,
      mediaServerId: mediaServerId,
      startTime: startTime,
      endTime: endTime
    }
  })
}

export function queryTaskList(params) {
  const { mediaServerId, isEnd } = params
  return request({
    method: 'get',
    url: `/api/cloud/record/task/list`,
    params: {
      mediaServerId: mediaServerId,
      isEnd: isEnd
    }

  })
}

export function deleteRecord(ids) {
  return request({
    method: 'delete',
    url: `/api/cloud/record/delete`,
    data: {
      ids: ids
    }

  })
}

export function queryList(params) {
  const { app, stream, query, startTime, endTime, mediaServerId, page, count, ascOrder } = params
  return request({
    method: 'get',
    url: `/api/cloud/record/list`,
    params: {
      app: app,
      stream: stream,
      query: query,
      startTime: startTime,
      endTime: endTime,
      mediaServerId: mediaServerId,
      page: page,
      count: count,
      ascOrder: ascOrder
    }
  })
}

