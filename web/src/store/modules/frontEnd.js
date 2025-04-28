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
  setSpeedForScan({ commit }, params) {
    return new Promise((resolve, reject) => {
      setSpeedForScan(params).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  setLeftForScan({ commit }, params) {
    return new Promise((resolve, reject) => {
      setLeftForScan(params).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  setRightForScan({ commit }, params) {
    return new Promise((resolve, reject) => {
      setRightForScan(params).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  startScan({ commit }, params) {
    return new Promise((resolve, reject) => {
      startScan(params).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  stopScan({ commit }, params) {
    return new Promise((resolve, reject) => {
      stopScan(params).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  addPointForCruise({ commit }, params) {
    return new Promise((resolve, reject) => {
      addPointForCruise(params).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  deletePointForCruise({ commit }, params) {
    return new Promise((resolve, reject) => {
      deletePointForCruise(params).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  setCruiseSpeed({ commit }, params) {
    return new Promise((resolve, reject) => {
      setCruiseSpeed(params).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  setCruiseTime({ commit }, params) {
    return new Promise((resolve, reject) => {
      setCruiseTime(params).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  startCruise({ commit }, params) {
    return new Promise((resolve, reject) => {
      startCruise(params).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  stopCruise({ commit }, params) {
    return new Promise((resolve, reject) => {
      stopCruise(params).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  addPreset({ commit }, params) {
    return new Promise((resolve, reject) => {
      addPreset(params).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  queryPreset({ commit }, params) {
    return new Promise((resolve, reject) => {
      queryPreset(params).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  callPreset({ commit }, params) {
    return new Promise((resolve, reject) => {
      callPreset(params).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  deletePreset({ commit }, params) {
    return new Promise((resolve, reject) => {
      deletePreset(params).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  auxiliary({ commit }, params) {
    return new Promise((resolve, reject) => {
      auxiliary(params).then(response => {
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
  iris({ commit }, params) {
    return new Promise((resolve, reject) => {
      iris(params).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  focus({ commit }, params) {
    return new Promise((resolve, reject) => {
      focus(params).then(response => {
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

