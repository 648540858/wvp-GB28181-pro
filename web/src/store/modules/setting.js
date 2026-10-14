import { getProfileList, getProfileContent, switchProfile, restartServer, getWebServerConfig, saveWebServerConfig, getSipServerConfig, saveSipServerConfig, getJtServerConfig, saveJtServerConfig, getMediaServerConfig, saveMediaServerConfig, getUserSettingConfig, saveUserSettingConfig, getAlarmTypes } from '@/api/setting'

const actions = {
  getProfileList({ commit }) {
    return new Promise((resolve, reject) => {
      getProfileList().then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  getProfileContent({ commit }, profileId) {
    return new Promise((resolve, reject) => {
      getProfileContent(profileId).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  switchProfile({ commit }, profileId) {
    return new Promise((resolve, reject) => {
      switchProfile(profileId).then(response => {
        resolve(response)
      }).catch(error => {
        reject(error)
      })
    })
  },
  restartServer({ commit }) {
    return new Promise((resolve, reject) => {
      restartServer().then(response => {
        resolve(response)
      }).catch(error => {
        reject(error)
      })
    })
  },
  getWebServerConfig({ commit }) {
    return new Promise((resolve, reject) => {
      getWebServerConfig().then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  saveWebServerConfig({ commit }, data) {
    return new Promise((resolve, reject) => {
      saveWebServerConfig(data).then(response => {
        resolve(response)
      }).catch(error => {
        reject(error)
      })
    })
  },
  getSipServerConfig({ commit }) {
    return new Promise((resolve, reject) => {
      getSipServerConfig().then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  saveSipServerConfig({ commit }, data) {
    return new Promise((resolve, reject) => {
      saveSipServerConfig(data).then(response => {
        resolve(response)
      }).catch(error => {
        reject(error)
      })
    })
  },
  getJtServerConfig({ commit }) {
    return new Promise((resolve, reject) => {
      getJtServerConfig().then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  saveJtServerConfig({ commit }, data) {
    return new Promise((resolve, reject) => {
      saveJtServerConfig(data).then(response => {
        resolve(response)
      }).catch(error => {
        reject(error)
      })
    })
  },
  getMediaServerConfig({ commit }) {
    return new Promise((resolve, reject) => {
      getMediaServerConfig().then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  saveMediaServerConfig({ commit }, data) {
    return new Promise((resolve, reject) => {
      saveMediaServerConfig(data).then(response => {
        resolve(response)
      }).catch(error => {
        reject(error)
      })
    })
  },
  getUserSettingConfig({ commit }) {
    return new Promise((resolve, reject) => {
      getUserSettingConfig().then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  saveUserSettingConfig({ commit }, data) {
    return new Promise((resolve, reject) => {
      saveUserSettingConfig(data).then(response => {
        resolve(response)
      }).catch(error => {
        reject(error)
      })
    })
  },
  getAlarmTypes({ commit }) {
    return new Promise((resolve, reject) => {
      getAlarmTypes().then(response => {
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
