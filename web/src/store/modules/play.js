import { broadcastStart, broadcastStop, play, stop } from '@/api/play'

const actions = {
  play({ commit }, [deviceId, channelId]) {
    return new Promise((resolve, reject) => {
      play(deviceId, channelId).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  stop({ commit }, [deviceId, channelId]) {
    return new Promise((resolve, reject) => {
      stop(deviceId, channelId).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  broadcastStart({ commit }, [deviceId, channelId, broadcastMode]) {
    return new Promise((resolve, reject) => {
      broadcastStart(deviceId, channelId, broadcastMode).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  broadcastStop({ commit }, [deviceId, channelId]) {
    return new Promise((resolve, reject) => {
      broadcastStop(deviceId, channelId).then(response => {
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

