const getters = {
  sidebar: state => state.app.sidebar,
  device: state => state.app.device,
  token: state => state.user.token,
  user: state => state.user.detail,
  user_lock: state => state.user.lock,
  user_menus: state => state.user.menus,
  user_perms: state => state.user.perms,
  permission_routes: state => state.permission.routes,
  permission_add_routes: state => state.permission.addRoutes
}
export default getters
