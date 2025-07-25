import {
  add,
  addChannel,
  connection,
  controlDoor,
  controlPlayback,
  deleteDevice,
  factoryReset,
  fillLight,
  getRecordTempUrl,
  linkDetection,
  play,
  ptz,
  queryAttribute,
  queryChannels,
  queryConfig,
  queryDeviceById,
  queryDevices,
  queryDriverInfo,
  queryMediaAttribute, queryMediaData,
  queryPosition,
  queryRecordList,
  reset,
  sendTextMessage,
  setConfig, setPhoneBook, shooting,
  startPlayback,
  stopPlay,
  stopPlayback,
  telephoneCallback,
  update,
  updateChannel,
  wiper
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
  },
  queryConfig({ commit }, phoneNumber) {
    return new Promise((resolve, reject) => {
      queryConfig(phoneNumber).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  setConfig({ commit }, data) {
    return new Promise((resolve, reject) => {
      setConfig(data).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  queryRecordList({ commit }, params) {
    return new Promise((resolve, reject) => {
      queryRecordList(params).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  startPlayback({ commit }, params) {
    return new Promise((resolve, reject) => {
      startPlayback(params).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  controlPlayback({ commit }, params) {
    return new Promise((resolve, reject) => {
      controlPlayback(params).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  stopPlayback({ commit }, params) {
    return new Promise((resolve, reject) => {
      stopPlayback(params).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  getRecordTempUrl({ commit }, params) {
    return new Promise((resolve, reject) => {
      getRecordTempUrl(params).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  queryAttribute({ commit }, phoneNumber) {
    return new Promise((resolve, reject) => {
      queryAttribute(phoneNumber).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  linkDetection({ commit }, phoneNumber) {
    return new Promise((resolve, reject) => {
      linkDetection(phoneNumber).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  queryPosition({ commit }, phoneNumber) {
    return new Promise((resolve, reject) => {
      queryPosition(phoneNumber).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  sendTextMessage({ commit }, data) {
    return new Promise((resolve, reject) => {
      sendTextMessage(data).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  telephoneCallback({ commit }, param) {
    return new Promise((resolve, reject) => {
      telephoneCallback(param).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  queryDriverInfo({ commit }, phoneNumber) {
    return new Promise((resolve, reject) => {
      queryDriverInfo(phoneNumber).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  factoryReset({ commit }, phoneNumber) {
    return new Promise((resolve, reject) => {
      factoryReset(phoneNumber).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  reset({ commit }, phoneNumber) {
    return new Promise((resolve, reject) => {
      reset(phoneNumber).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  connection({ commit }, data) {
    return new Promise((resolve, reject) => {
      connection(data).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  controlDoor({ commit }, param) {
    return new Promise((resolve, reject) => {
      controlDoor(param).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  queryMediaAttribute({ commit }, phoneNumber) {
    return new Promise((resolve, reject) => {
      queryMediaAttribute(phoneNumber).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  setPhoneBook({ commit }, data) {
    return new Promise((resolve, reject) => {
      setPhoneBook(data).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  queryMediaData({ commit }, data) {
    return new Promise((resolve, reject) => {
      queryMediaData(data).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  shooting({ commit }, data) {
    return new Promise((resolve, reject) => {
      shooting(data).then(response => {
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

