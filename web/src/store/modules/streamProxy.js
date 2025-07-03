import { add, play, queryFfmpegCmdList, queryList, remove, save, stopPlay, update } from '@/api/streamProxy'

const actions = {
  queryFfmpegCmdList({ commit }, mediaServerId) {
    return new Promise((resolve, reject) => {
      queryFfmpegCmdList(mediaServerId).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  save({ commit }, formData) {
    return new Promise((resolve, reject) => {
      save(formData).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
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
  queryList({ commit }, params) {
    return new Promise((resolve, reject) => {
      queryList(params).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  play({ commit }, id) {
    return new Promise((resolve, reject) => {
      play(id).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  stopPlay({ commit }, id) {
    return new Promise((resolve, reject) => {
      stopPlay(id).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  remove({ commit }, id) {
    return new Promise((resolve, reject) => {
      remove(id).then(response => {
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

