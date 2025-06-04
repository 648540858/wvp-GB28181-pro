import Storage from 'good-storage'

const TokenKey = 'data_ui_token'

export function getToken() {
  return Storage.get(TokenKey)
}

export function setToken(token) {
  return Storage.set(TokenKey, token)
}

export function removeToken() {
  return Storage.remove(TokenKey)
}
