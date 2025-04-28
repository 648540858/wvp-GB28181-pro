import crypto from 'crypto'
import {
  add,
  changePassword,
  changePasswordForAdmin,
  changePushKey,
  getUserInfo,
  login,
  logout,
  queryList,
  removeById
} from '@/api/user'
import {
  getToken,
  setToken,
  setName,
  removeToken,
  removeName,
  setServerId,
  removeServerId
} from '@/utils/auth'
import { resetRouter } from '@/router'

const getDefaultState = () => {
  return {
    token: getToken(),
    name: '',
    serverId: ''
  }
}

const state = getDefaultState()

const mutations = {
  RESET_STATE: (state) => {
    Object.assign(state, getDefaultState())
  },
  SET_TOKEN: (state, token) => {
    state.token = token
  },
  SET_NAME: (state, name) => {
    state.name = name
  },
  SET_SERVER_ID: (state, serverId) => {
    state.serverId = serverId
  }
}

const actions = {
  // user login
  login({ commit }, userInfo) {
    const { username, password } = userInfo
    return new Promise((resolve, reject) => {
      login({
        username: username.trim(),
        password: crypto.createHash('md5').update(password, 'utf8').digest('hex')
      }).then(response => {
        const { data } = response
        commit('SET_TOKEN', data.accessToken)
        commit('SET_NAME', data.username)
        commit('SET_SERVER_ID', data.serverId)
        setToken(data.accessToken)
        setName(data.username)
        setServerId(data.serverId)
        resolve()
      }).catch(error => {
        reject(error)
      })
    })
  },
  // user logout
  logout({ commit, state }) {
    return new Promise((resolve, reject) => {
      logout(state.token).then(() => {
        removeToken()
        removeServerId()
        removeName()
        resetRouter()
        commit('RESET_STATE')
        resolve()
      }).catch(error => {
        reject(error)
      })
    })
  },

  // remove token
  resetToken({ commit }) {
    return new Promise(resolve => {
      removeToken() // must remove  token  first
      commit('RESET_STATE')
      resolve()
    })
  },

  getUserInfo({ commit }) {
    return new Promise((resolve, reject) => {
      getUserInfo().then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },

  changePushKey({ commit }, params) {
    return new Promise((resolve, reject) => {
      changePushKey(params).then(response => {
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

  removeById({ commit }, id) {
    return new Promise((resolve, reject) => {
      removeById(id).then(response => {
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
  },

  changePassword({ commit }, params) {
    return new Promise((resolve, reject) => {
      changePassword(params).then(response => {
        const { data } = response
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },

  changePasswordForAdmin({ commit }, params) {
    return new Promise((resolve, reject) => {
      changePasswordForAdmin(params).then(response => {
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
  state,
  mutations,
  actions
}

