import Cookies from 'js-cookie'

const TokenKey = 'wvp_token'
const NameKey = 'wvp_username'
const serverIdKey = 'wvp_server_id'

export function getToken() {
  console.log('Getting token...')
  return Cookies.get(TokenKey)
}

export function setToken(token) {
  return Cookies.set(TokenKey, token)
}

export function removeToken() {
  return Cookies.remove(TokenKey)
}

export function getName() {
  return Cookies.get(NameKey)
}

export function setName(name) {
  return Cookies.set(NameKey, name)
}

export function removeName() {
  return Cookies.remove(NameKey)
}

export function getServerId() {
  return Cookies.get(serverIdKey)
}

export function setServerId(serverId) {
  return Cookies.set(serverIdKey, serverId)
}

export function removeServerId() {
  return Cookies.remove(serverIdKey)
}
