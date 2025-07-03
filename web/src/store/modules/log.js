import { queryList } from '@/api/log'

const actions = {
  queryList({ commit }, params) {
    return new Promise((resolve, reject) => {
      queryList(params).then(response => {
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

