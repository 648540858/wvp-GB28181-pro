import {
  addPointForCruise, addPreset, auxiliary, callPreset, deletePointForCruise, deletePreset, focus, iris, ptz,
  queryPreset, setCruiseSpeed, setCruiseTime,
  setLeftForScan,
  setRightForScan,
  setSpeedForScan, startCruise,
  startScan, stopCruise,
  stopScan, wiper
} from '@/api/frontEnd'

const actions = {
  setSpeedForScan({ commit }, [deviceId, channelDeviceId, scanId, speed]) {
    return new Promise((resolve, reject) => {
      setSpeedForScan(deviceId, channelDeviceId, scanId, speed).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  setLeftForScan({ commit }, [deviceId, channelDeviceId, scanId]) {
    return new Promise((resolve, reject) => {
      setLeftForScan(deviceId, channelDeviceId, scanId).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  setRightForScan({ commit }, [deviceId, channelDeviceId, scanId]) {
    return new Promise((resolve, reject) => {
      setRightForScan(deviceId, channelDeviceId, scanId).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  startScan({ commit }, [deviceId, channelDeviceId, scanId]) {
    return new Promise((resolve, reject) => {
      startScan(deviceId, channelDeviceId, scanId).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  stopScan({ commit }, [deviceId, channelDeviceId, scanId]) {
    return new Promise((resolve, reject) => {
      stopScan(deviceId, channelDeviceId, scanId).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  queryPreset({ commit }, [deviceId, channelDeviceId]) {
    return new Promise((resolve, reject) => {
      queryPreset(deviceId, channelDeviceId).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  addPointForCruise({ commit }, [deviceId, channelDeviceId, cruiseId, presetId]) {
    return new Promise((resolve, reject) => {
      addPointForCruise(deviceId, channelDeviceId, cruiseId, presetId).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  deletePointForCruise({ commit }, [deviceId, channelDeviceId, cruiseId, presetId]) {
    return new Promise((resolve, reject) => {
      deletePointForCruise(deviceId, channelDeviceId, cruiseId, presetId).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  setCruiseSpeed({ commit }, [deviceId, channelDeviceId, cruiseId, cruiseSpeed]) {
    return new Promise((resolve, reject) => {
      setCruiseSpeed(deviceId, channelDeviceId, cruiseId, cruiseSpeed).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  setCruiseTime({ commit }, [deviceId, channelDeviceId, cruiseId, cruiseTime]) {
    return new Promise((resolve, reject) => {
      setCruiseTime(deviceId, channelDeviceId, cruiseId, cruiseTime).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  startCruise({ commit }, [deviceId, channelDeviceId, cruiseId]) {
    return new Promise((resolve, reject) => {
      startCruise(deviceId, channelDeviceId, cruiseId).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  stopCruise({ commit }, [deviceId, channelDeviceId, cruiseId]) {
    return new Promise((resolve, reject) => {
      stopCruise(deviceId, channelDeviceId, cruiseId).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  addPreset({ commit }, [deviceId, channelDeviceId, presetId]) {
    return new Promise((resolve, reject) => {
      addPreset(deviceId, channelDeviceId, presetId).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  callPreset({ commit }, [deviceId, channelDeviceId, presetId]) {
    return new Promise((resolve, reject) => {
      callPreset(deviceId, channelDeviceId, presetId).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  deletePreset({ commit }, [deviceId, channelDeviceId, presetId]) {
    return new Promise((resolve, reject) => {
      deletePreset(deviceId, channelDeviceId, presetId).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  auxiliary({ commit }, [deviceId, channelDeviceId, command, switchId]) {
    return new Promise((resolve, reject) => {
      auxiliary(deviceId, channelDeviceId, command, switchId).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  wiper({ commit }, [deviceId, channelDeviceId, command]) {
    return new Promise((resolve, reject) => {
      wiper(deviceId, channelDeviceId, command).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  ptz({ commit }, [deviceId, channelId, command, horizonSpeed, verticalSpeed, zoomSpeed]) {
    return new Promise((resolve, reject) => {
      ptz(deviceId, channelId, command, horizonSpeed, verticalSpeed, zoomSpeed).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  iris({ commit }, [deviceId, channelId, command, speed]) {
    return new Promise((resolve, reject) => {
      iris(deviceId, channelId, command, speed).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  focus({ commit }, [deviceId, channelDeviceId, command, speed]) {
    return new Promise((resolve, reject) => {
      iris(deviceId, channelDeviceId, command, speed).then(response => {
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

