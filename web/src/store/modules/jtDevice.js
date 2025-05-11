import { deleteDeviceById, queryChannels, queryDeviceById, queryDevices } from '@/api/jtDevice'

const actions = {
  queryDevices({ commit }, params) {
    return new Promise((resolve, reject) => {
      queryDevices(params).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  queryDeviceById({ commit }, deviceId) {
    return new Promise((resolve, reject) => {
      queryDeviceById(deviceId).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  deleteDeviceById({ commit }, deviceId) {
    return new Promise((resolve, reject) => {
      deleteDeviceById(deviceId).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  queryChannels({ commit }, params) {
    return new Promise((resolve, reject) => {
      queryChannels(params).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  }
}

export default {
  namespaced: true,
  actions
}

