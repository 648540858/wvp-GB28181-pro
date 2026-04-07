import { getAlarmList, deleteAlarms, clearAlarms } from '@/api/alarm'

const actions = {
  getAlarmList({ commit }, params) {
    return new Promise((resolve, reject) => {
      getAlarmList(params).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  deleteAlarms({ commit }, ids) {
    return new Promise((resolve, reject) => {
      deleteAlarms(ids).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  clearAlarms({ commit }, params) {
    return new Promise((resolve, reject) => {
      clearAlarms(params).then(response => {
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
