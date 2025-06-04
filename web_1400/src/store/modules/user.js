import { login, logout, getRoutes } from '@/api/system/user'
import { getToken, setToken, removeToken } from '@/utils/auth'
import { resetRouter } from '@/router'
import Storage from 'good-storage'

const getDefaultState = () => {
  return {
    token: getToken(),
    detail: {},
    menus: [],
    perms: [],
    lock: ''
  }
}

const state = {
  token: getToken(),
  detail: Storage.get('data_ui_user_detail') || {},
  menus: [],
  perms: Storage.get('data_ui_user_perms') || [],
  lock: Storage.get('data_ui_user_lock') || ''
}

const mutations = {
  RESET_STATE: (state) => {
    Object.assign(state, getDefaultState())
  },
  SET_TOKEN: (state, token) => {
    state.token = token
  },
  SET_DETAIL: (state, detail) => {
    state.detail = detail
  },
  SET_MENUS: (state, menus) => {
    state.menus = menus
  },
  SET_PERMS: (state, perms) => {
    state.perms = perms
  },
  SET_LOCK: (state, lock) => {
    Storage.set('data_ui_user_lock', lock)
    state.lock = lock
  }
}

const actions = {
  login({ commit }, userInfo) {
    const { username, password } = userInfo
    return new Promise((resolve, reject) => {
      login(username, password).then(response => {
        const { access_token, username, nickname, user_id, user_dept, user_post, user_role } = response
        commit('SET_TOKEN', access_token)
        setToken(access_token)
        const detail = {
          id: user_id || '',
          username: username || '',
          nickname: nickname || '',
          dept: user_dept || '',
          roles: user_role ? user_role.map(role => role.id) : [],
          posts: user_post || []
        }
        commit('SET_DETAIL', detail)
        Storage.set('data_ui_user_detail', detail)
        resolve()
      }).catch(error => {
        reject(error)
      })
    })
  },
  setUserDetail({ commit }, userInfo) {
    const { access_token, username, nickname, user_id, user_dept, user_post, user_role } = userInfo
    commit('SET_TOKEN', access_token)
    setToken(access_token)
    const detail = {
      id: user_id || '',
      username: username || '',
      nickname: nickname || '',
      dept: user_dept || '',
      roles: user_role ? user_role.map(role => role.id) : [],
      posts: user_post || []
    }
    commit('SET_DETAIL', detail)
    Storage.set('data_ui_user_detail', detail)
  },

  getInfo({ commit }) {
    return new Promise((resolve, reject) => {
      getRoutes().then(response => {
        const { data } = response
        const perms = data.perms || []
        const routes = data.routes || []
        commit('SET_MENUS', routes)
        Storage.set('data_ui_user_menus', routes)
        commit('SET_PERMS', perms)
        Storage.set('data_ui_user_perms', perms)
        resolve()
      }).catch(error => {
        reject(error)
      })
    })
  },

  logout({ commit, state }) {
    return new Promise((resolve, reject) => {
      logout(state.token).then(() => {
        removeToken()
        resetRouter()
        commit('RESET_STATE')
        resolve()
      }).catch(error => {
        reject(error)
      })
    })
  },

  resetToken({ commit }) {
    return new Promise(resolve => {
      removeToken()
      commit('RESET_STATE')
      resolve()
    })
  }
}

export default {
  namespaced: true,
  state,
  mutations,
  actions
}

