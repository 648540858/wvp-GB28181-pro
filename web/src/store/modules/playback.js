import { pause, play, resume, setSpeed, stop } from '@/api/playback'

const actions = {
  play({ commit }, data) {
    return new Promise((resolve, reject) => {
      play(data).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  resume({ commit }, streamId) {
    return new Promise((resolve, reject) => {
      resume(streamId).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  pause({ commit }, streamId) {
    return new Promise((resolve, reject) => {
      pause(streamId).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  setSpeed({ commit }, param) {
    return new Promise((resolve, reject) => {
      setSpeed(param).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  stop({ commit }, [deviceId, channelId, streamId]) {
    return new Promise((resolve, reject) => {
      stop(deviceId, channelId, streamId).then(response => {
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

