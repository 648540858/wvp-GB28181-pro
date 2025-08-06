import {
  update,
  add,
  reset,
  queryOne,
  addDeviceToGroup,
  deleteDeviceFromGroup,
  addDeviceToRegion,
  deleteDeviceFromRegion,
  getCivilCodeList,
  getParentList,
  getUnusualParentList,
  clearUnusualParentList,
  getUnusualCivilCodeList,
  clearUnusualCivilCodeList,
  getIndustryList,
  getTypeList,
  getNetworkIdentificationList,
  playChannel,
  addToRegion,
  deleteFromRegion,
  addToGroup,
  deleteFromGroup,
  getList,
  addPointForCruise,
  addPreset,
  auxiliary,
  callPreset,
  deletePointForCruise,
  deletePreset,
  focus,
  iris,
  ptz,
  queryPreset,
  setCruiseSpeed,
  setCruiseTime,
  setLeftForScan,
  setRightForScan,
  setSpeedForScan,
  startCruise,
  startScan,
  stopCruise,
  stopScan,
  wiper,
  stopPlayChannel,
  queryRecord,
  playback,
  stopPlayback,
  pausePlayback,
  resumePlayback,
  seekPlayback, speedPlayback
} from '@/api/commonChannel'

const actions = {
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
  reset({ commit }, id) {
    return new Promise((resolve, reject) => {
      reset(id).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  queryOne({ commit }, id) {
    return new Promise((resolve, reject) => {
      queryOne(id).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  addDeviceToGroup({ commit }, params) {
    return new Promise((resolve, reject) => {
      addDeviceToGroup(params).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  addToGroup({ commit }, params) {
    return new Promise((resolve, reject) => {
      addToGroup(params).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  deleteDeviceFromGroup({ commit }, deviceIds) {
    return new Promise((resolve, reject) => {
      deleteDeviceFromGroup(deviceIds).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  deleteFromGroup({ commit }, channels) {
    return new Promise((resolve, reject) => {
      deleteFromGroup(channels).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  addDeviceToRegion({ commit }, params) {
    return new Promise((resolve, reject) => {
      addDeviceToRegion(params).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  addToRegion({ commit }, params) {
    return new Promise((resolve, reject) => {
      addToRegion(params).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  deleteDeviceFromRegion({ commit }, deviceIds) {
    return new Promise((resolve, reject) => {
      deleteDeviceFromRegion(deviceIds).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  deleteFromRegion({ commit }, channels) {
    return new Promise((resolve, reject) => {
      deleteFromRegion(channels).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  getCivilCodeList({ commit }, params) {
    return new Promise((resolve, reject) => {
      getCivilCodeList(params).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  getParentList({ commit }, params) {
    return new Promise((resolve, reject) => {
      getParentList(params).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  getUnusualParentList({ commit }, params) {
    return new Promise((resolve, reject) => {
      getUnusualParentList(params).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  clearUnusualParentList({ commit }, params) {
    return new Promise((resolve, reject) => {
      clearUnusualParentList(params).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  getUnusualCivilCodeList({ commit }, params) {
    return new Promise((resolve, reject) => {
      getUnusualCivilCodeList(params).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  clearUnusualCivilCodeList({ commit }, params) {
    return new Promise((resolve, reject) => {
      clearUnusualCivilCodeList(params).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  getIndustryList({ commit }) {
    return new Promise((resolve, reject) => {
      getIndustryList().then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  getTypeList({ commit }) {
    return new Promise((resolve, reject) => {
      getTypeList().then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  getNetworkIdentificationList({ commit }) {
    return new Promise((resolve, reject) => {
      getNetworkIdentificationList().then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  playChannel({ commit }, channelId) {
    return new Promise((resolve, reject) => {
      playChannel(channelId).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  stopPlayChannel({ commit }, channelId) {
    return new Promise((resolve, reject) => {
      stopPlayChannel(channelId).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  getList({ commit }, param) {
    return new Promise((resolve, reject) => {
      getList(param).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
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
  },
  queryRecord({ commit }, params) {
    return new Promise((resolve, reject) => {
      queryRecord(params).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  playback({ commit }, params) {
    return new Promise((resolve, reject) => {
      playback(params).then(response => {
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
  pausePlayback({ commit }, params) {
    return new Promise((resolve, reject) => {
      pausePlayback(params).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  resumePlayback({ commit }, params) {
    return new Promise((resolve, reject) => {
      resumePlayback(params).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  seekPlayback({ commit }, params) {
    return new Promise((resolve, reject) => {
      seekPlayback(params).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  speedPlayback({ commit }, params) {
    return new Promise((resolve, reject) => {
      speedPlayback(params).then(response => {
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

