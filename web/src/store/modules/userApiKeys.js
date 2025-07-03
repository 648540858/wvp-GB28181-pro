import { add, disable, enable, queryList, remark, remove, reset } from '@/api/userApiKey'

const actions = {
  remark({ commit }, params) {
    return new Promise((resolve, reject) => {
      remark(params).then(response => {
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
  enable({ commit }, id) {
    return new Promise((resolve, reject) => {
      enable(id).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  disable({ commit }, id) {
    return new Promise((resolve, reject) => {
      disable(id).then(response => {
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
  add({ commit }, params) {
    return new Promise((resolve, reject) => {
      add(params).then(response => {
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

