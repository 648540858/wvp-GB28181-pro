import {
  getTreeList,
  deleteRegion,
  description,
  addByCivilCode,
  queryChildListInBase,
  update,
  add,
  queryPath
} from '@/api/region'

const actions = {
  getTreeList({ commit }, data) {
    return new Promise((resolve, reject) => {
      getTreeList(data).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  deleteRegion({ commit }, id) {
    return new Promise((resolve, reject) => {
      deleteRegion(id).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  description({ commit }, civilCode) {
    return new Promise((resolve, reject) => {
      description(civilCode).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  addByCivilCode({ commit }, civilCode) {
    return new Promise((resolve, reject) => {
      addByCivilCode(civilCode).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  queryChildListInBase({ commit }, parent) {
    return new Promise((resolve, reject) => {
      queryChildListInBase(parent).then(response => {
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
  queryPath({ commit }, deviceId) {
    return new Promise((resolve, reject) => {
      queryPath(deviceId).then(response => {
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

