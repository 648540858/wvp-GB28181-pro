import axios from 'axios';

export const tree = (deviceId) => {
  return axios({
    url: `/api/device/query/${deviceId}/tree`,
    method: 'get'
  })
}

export const deviceList = (page, count) => {
  return axios({
    method: 'get',
    url:`/api/device/query/devices`,
    params: {
      page,
      count
    }
  })
}