import request from '@/utils/request'

export function getPlan(id) {
  return request({
    method: 'get',
    url: '/api/record/plan/get',
    params: {
      planId: id
    }
  })
}

export function addPlan(params) {
  const { name, planList } = params
  return request({

    method: 'post',
    url: '/api/record/plan/add',
    data: {
      name: name,
      planItemList: planList
    }
  })
}

export function update(params) {
  const { id, name, planList } = params
  return request({
    method: 'post',
    url: '/api/record/plan/update',
    data: {
      id: id,
      name: name,
      planItemList: planList
    }
  })
}

export function queryList(params) {
  const { page, count, query } = params
  return request({
    method: 'get',
    url: `/api/record/plan/query`,
    params: {
      page: page,
      count: count,
      query: query
    }
  })
}

export function deletePlan(id) {
  return request({
    method: 'delete',
    url: '/api/record/plan/delete',
    params: {
      planId: id
    }
  })
}

export function queryChannelList(params) {
  const { page, count, channelType, query, online, planId , hasLink } = params
  return request({
    method: 'get',
    url: `/api/record/plan/channel/list`,
    params: {
      page: page,
      count: count,
      query: query,
      online: online,
      channelType: channelType,
      planId: planId,
      hasLink: hasLink
    }
  })
}

export function linkPlan(data) {
  return request({
    method: 'post',
    url: `/api/record/plan/link`,
    data: data
  })
}
