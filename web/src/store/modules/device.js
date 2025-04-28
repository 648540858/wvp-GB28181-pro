import {
  add, changeChannelAudio,
  deleteDevice,
  deviceRecord,
  queryBasicParam,
  queryChannelOne,
  queryChannels, queryChannelTree, queryDeviceOne,
  queryDevices,
  queryDeviceSyncStatus, queryDeviceTree,
  resetGuard,
  setGuard,
  subscribeCatalog,
  subscribeMobilePosition,
  sync, update, updateChannelStreamIdentification,
  updateDeviceTransport
} from '@/api/device'

const actions = {
  queryDeviceSyncStatus({ commit }, deviceId) {
    return new Promise((resolve, reject) => {
      queryDeviceSyncStatus(deviceId).then(response => {
        // const {data, code, msg} = response
        resolve(response)
      }).catch(error => {
        reject(error)
      })
    })
  },
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
  sync({ commit }, deviceId) {
    return new Promise((resolve, reject) => {
      sync(deviceId).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  updateDeviceTransport({ commit }, [deviceId, streamMode]) {
    return new Promise((resolve, reject) => {
      updateDeviceTransport(deviceId, streamMode).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  setGuard({ commit }, deviceId) {
    return new Promise((resolve, reject) => {
      setGuard(deviceId).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  resetGuard({ commit }, deviceId) {
    return new Promise((resolve, reject) => {
      resetGuard(deviceId).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  subscribeCatalog({ commit }, params) {
    return new Promise((resolve, reject) => {
      subscribeCatalog(params).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  subscribeMobilePosition({ commit }, params) {
    return new Promise((resolve, reject) => {
      subscribeMobilePosition(params).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  queryBasicParam({ commit }, deviceId) {
    return new Promise((resolve, reject) => {
      queryBasicParam(deviceId).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  queryChannelOne({ commit }, params) {
    return new Promise((resolve, reject) => {
      queryChannelOne(params).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  queryChannels({ commit }, [deviceId, params]) {
    return new Promise((resolve, reject) => {
      queryChannels(deviceId, params).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  deviceRecord({ commit }, params) {
    return new Promise((resolve, reject) => {
      deviceRecord(params).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  querySubChannels({ commit }, [params, deviceId, parentChannelId]) {
    return new Promise((resolve, reject) => {
      deviceRecord(params, deviceId, parentChannelId).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  queryChannelTree({ commit }, params) {
    return new Promise((resolve, reject) => {
      queryChannelTree(params).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  changeChannelAudio({ commit }, params) {
    return new Promise((resolve, reject) => {
      changeChannelAudio(params).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  updateChannelStreamIdentification({ commit }, params) {
    return new Promise((resolve, reject) => {
      updateChannelStreamIdentification(params).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  update({ commit }, formData) {
    return new Promise((resolve, reject) => {
      update(formData).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  add({ commit }, formData) {
    return new Promise((resolve, reject) => {
      add(formData).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  queryDeviceOne({ commit }, deviceId) {
    return new Promise((resolve, reject) => {
      queryDeviceOne(deviceId).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  queryDeviceTree({ commit }, params, deviceId) {
    return new Promise((resolve, reject) => {
      queryDeviceTree(params, deviceId).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  deleteDevice({ commit }, deviceId) {
    return new Promise((resolve, reject) => {
      deleteDevice(deviceId).then(response => {
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

