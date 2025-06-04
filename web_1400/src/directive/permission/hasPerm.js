import store from '@/store'

export default {
  inserted(el, binding, vnode) {
    const { value } = binding
    const allPermission = '*:*:*'
    const permissions = store.getters && store.getters.user_perms

    if (value && value instanceof Array && value.length > 0) {
      const permissionFlag = value

      const hasPermissions = permissions.some(permission => {
        return allPermission === permission || permissionFlag.includes(permission)
      })

      if (!hasPermissions) {
        el.parentNode && el.parentNode.removeChild(el)
      }
    } else {
      throw new Error('请设置操作权限标签值')
    }
  }
}
