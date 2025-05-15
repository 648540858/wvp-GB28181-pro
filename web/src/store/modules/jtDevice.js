import {
  add,
  addChannel, deleteDevice,
  deleteDeviceById, fillLight,
  play, ptz,
  queryChannels,
  queryDeviceById,
  queryDevices,
  stopPlay, update,
  updateChannel, wiper
} from '@/api/jtDevice'

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
  deleteDevice({ commit }, phoneNumber) {
    return new Promise((resolve, reject) => {
      deleteDevice(phoneNumber).then(response => {
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
  },
  updateChannel({ commit }, data) {
    return new Promise((resolve, reject) => {
      updateChannel(data).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  addChannel({ commit }, data) {
    return new Promise((resolve, reject) => {
      addChannel(data).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  ptz({ commit }, params) {
    return new Promise((resolve, reject) => {
      ptz(params).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  wiper({ commit }, params) {
    return new Promise((resolve, reject) => {
      wiper(params).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  fillLight({ commit }, params) {
    return new Promise((resolve, reject) => {
      fillLight(params).then(response => {
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

