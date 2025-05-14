import { deleteDeviceById, play, queryChannels, queryDeviceById, queryDevices, stopPlay } from '@/api/jtDevice'
import { add } from '@/api/user'
import { update } from '@/api/group'

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
  add({ commit }, params) {
    return new Promise((resolve, reject) => {
      add(params).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  update({ commit }, params) {
    return new Promise((resolve, reject) => {
      update(params).then(response => {
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
  },
  play({ commit }, params) {
    return new Promise((resolve, reject) => {
      play(params).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  stopPlay({ commit }, params) {
    return new Promise((resolve, reject) => {
      stopPlay(params).then(response => {
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

