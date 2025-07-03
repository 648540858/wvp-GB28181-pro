import { addPlan, deletePlan, getPlan, linkPlan, queryChannelList, queryList, update } from '@/api/recordPlan'

const actions = {
  getPlan({ commit }, id) {
    return new Promise((resolve, reject) => {
      getPlan(id).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  addPlan({ commit }, params) {
    return new Promise((resolve, reject) => {
      addPlan(params).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  update({ commit }, params) {
    return new Promise((resolve, reject) => {
      update(params).then(response => {
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
  deletePlan({ commit }, id) {
    return new Promise((resolve, reject) => {
      deletePlan(id).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  queryChannelList({ commit }, params) {
    return new Promise((resolve, reject) => {
      queryChannelList(params).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  linkPlan({ commit }, data) {
    return new Promise((resolve, reject) => {
      linkPlan(data).then(response => {
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

