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
  getNetworkIdentificationList, playChannel, addToRegion, deleteFromRegion, addToGroup, deleteFromGroup
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
  }
}

export default {
  namespaced: true,
  actions
}

