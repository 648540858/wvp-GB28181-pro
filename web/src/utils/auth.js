import Cookies from 'js-cookie'

const TokenKey = 'wvp_token'
const NameKey = 'wvp_username'
const serverIdKey = 'wvp_server_id'
const defaultPasswordKey = 'wvp_default_password'
const expires = 30

export function getToken() {
  console.log('Getting token...')
  return Cookies.get(TokenKey)
}

export function setToken(token) {
  return Cookies.set(TokenKey, token, {expires: expires})
}

export function removeToken() {
  return Cookies.remove(TokenKey)
}

export function getName() {
  return Cookies.get(NameKey)
}

export function setName(name) {
  return Cookies.set(NameKey, name, {expires: expires})
}

export function removeName() {
  return Cookies.remove(NameKey)
}

export function getServerId() {
  return Cookies.get(serverIdKey)
}

export function setServerId(serverId) {
  return Cookies.set(serverIdKey, serverId, {expires: expires})
}

export function removeServerId() {
  return Cookies.remove(serverIdKey)
}

export function getDefaultPassword() {
  return Cookies.get(defaultPasswordKey) === 'true'
}

export function setDefaultPassword(defaultPassword) {
  return Cookies.set(defaultPasswordKey, !!defaultPassword ? 'true' : 'false', {expires: expires})
}

export function removeDefaultPassword() {
  return Cookies.remove(defaultPasswordKey)
}
