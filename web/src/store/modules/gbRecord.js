
import { query, queryDownloadProgress, startDownLoad, stopDownLoad } from '@/api/gbRecord'

const actions = {
  query({ commit }, param) {
    return new Promise((resolve, reject) => {
      query(param).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  startDownLoad({ commit }, param) {
    return new Promise((resolve, reject) => {
      startDownLoad(param).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  stopDownLoad({ commit }, deviceId, channelId, streamId) {
    return new Promise((resolve, reject) => {
      stopDownLoad(deviceId, channelId, streamId).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  queryDownloadProgress({ commit }, param) {
    return new Promise((resolve, reject) => {
      queryDownloadProgress(param).then(response => {
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

