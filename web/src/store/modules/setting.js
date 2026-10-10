import { getProfileList, getProfileContent, switchProfile, restartServer } from '@/api/setting'

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
  }
}

export default {
  namespaced: true,
  actions
}
