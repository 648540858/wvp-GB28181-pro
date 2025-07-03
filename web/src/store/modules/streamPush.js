import { saveToGb, add, update, queryList, play, remove, removeFormGb, batchRemove } from '@/api/streamPush'

const actions = {
  saveToGb({ commit }, formData) {
    return new Promise((resolve, reject) => {
      saveToGb(formData).then(response => {
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
  remove({ commit }, id) {
    return new Promise((resolve, reject) => {
      remove(id).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  removeFormGb({ commit }, formData) {
    return new Promise((resolve, reject) => {
      removeFormGb(formData).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  batchRemove({ commit }, ids) {
    return new Promise((resolve, reject) => {
      batchRemove(ids).then(response => {
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

