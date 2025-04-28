import {
  checkMediaServer,
  checkMediaServerRecord, deleteMediaServer, getMediaInfo,
  getMediaServer,
  getMediaServerList, getMediaServerLoad,
  getOnlineMediaServerList, getResourceInfo, getSystemConfig, getSystemInfo, info, saveMediaServer
} from '@/api/server'

const actions = {
  getOnlineMediaServerList({ commit }) {
    return new Promise((resolve, reject) => {
      getOnlineMediaServerList().then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  getMediaServerList({ commit }) {
    return new Promise((resolve, reject) => {
      getMediaServerList().then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  getMediaServer({ commit }, id) {
    return new Promise((resolve, reject) => {
      getMediaServer(id).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  checkMediaServer({ commit }, params) {
    return new Promise((resolve, reject) => {
      checkMediaServer(params).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  checkMediaServerRecord({ commit }, params) {
    return new Promise((resolve, reject) => {
      checkMediaServerRecord(params).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  saveMediaServer({ commit }, formData) {
    return new Promise((resolve, reject) => {
      saveMediaServer(formData).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  deleteMediaServer({ commit }, id) {
    return new Promise((resolve, reject) => {
      deleteMediaServer(id).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  getSystemConfig({ commit }) {
    return new Promise((resolve, reject) => {
      getSystemConfig().then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  getMediaInfo({ commit }, params) {
    return new Promise((resolve, reject) => {
      getMediaInfo(params).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  getSystemInfo({ commit }) {
    return new Promise((resolve, reject) => {
      getSystemInfo().then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  getMediaServerLoad({ commit }) {
    return new Promise((resolve, reject) => {
      getMediaServerLoad().then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  getResourceInfo({ commit }) {
    return new Promise((resolve, reject) => {
      getResourceInfo().then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  info({ commit }) {
    return new Promise((resolve, reject) => {
      info().then(response => {
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

