import {
  add,
  addChannel, addChannelByDevice,
  exit,
  getChannelList,
  getServerConfig,
  pushChannel,
  query,
  remove, removeChannel, removeChannelByDevice,
  update, updateCustomChannel
} from '@/api/platform'

const actions = {
  update({ commit }, data) {
    return new Promise((resolve, reject) => {
      update(data).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  add({ commit }, data) {
    return new Promise((resolve, reject) => {
      add(data).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  exit({ commit }, deviceGbId) {
    return new Promise((resolve, reject) => {
      exit(deviceGbId).then(response => {
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
  pushChannel({ commit }, id) {
    return new Promise((resolve, reject) => {
      pushChannel(id).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  getServerConfig({ commit }) {
    return new Promise((resolve, reject) => {
      getServerConfig().then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  query({ commit }, params) {
    return new Promise((resolve, reject) => {
      query(params).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  getChannelList({ commit }, params) {
    return new Promise((resolve, reject) => {
      getChannelList(params).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  addChannel({ commit }, params) {
    return new Promise((resolve, reject) => {
      addChannel(params).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  addChannelByDevice({ commit }, params) {
    return new Promise((resolve, reject) => {
      addChannelByDevice(params).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  removeChannelByDevice({ commit }, params) {
    return new Promise((resolve, reject) => {
      removeChannelByDevice(params).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  removeChannel({ commit }, params) {
    return new Promise((resolve, reject) => {
      removeChannel(params).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  updateCustomChannel({ commit }, data) {
    return new Promise((resolve, reject) => {
      updateCustomChannel(data).then(response => {
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

