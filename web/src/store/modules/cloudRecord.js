import {
  addTask, deleteRecord,
  getPlayPath,
  loadRecord,
  queryList,
  queryListByData,
  queryTaskList,
  seek,
  speed
} from '@/api/cloudRecord'

const actions = {
  getPlayPath({ commit }, id) {
    return new Promise((resolve, reject) => {
      getPlayPath(id).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  loadRecord({ commit }, params) {
    return new Promise((resolve, reject) => {
      loadRecord(params).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  seek({ commit }, params) {
    return new Promise((resolve, reject) => {
      seek(params).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  speed({ commit }, params) {
    return new Promise((resolve, reject) => {
      speed(params).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  queryListByData({ commit }, params) {
    return new Promise((resolve, reject) => {
      queryListByData(params).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  addTask({ commit }, params) {
    return new Promise((resolve, reject) => {
      addTask(params).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  queryTaskList({ commit }, params) {
    return new Promise((resolve, reject) => {
      queryTaskList(params).then(response => {
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
  deleteRecord({ commit }, ids) {
    return new Promise((resolve, reject) => {
      deleteRecord(ids).then(response => {
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

