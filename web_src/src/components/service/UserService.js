
export default {

  /**
   * 存储用户信息
   * @param username
   * @param token
   */
  setUser(user){
    localStorage.setItem("wvp-user", JSON.stringify(user));
  },

  /**
   * 获取用户
   */
  getUser(){
    return JSON.parse(localStorage.getItem("wvp-user"));
  },


  /**
   * 获取登录token
   */
  getToken(){
    return localStorage.getItem("wvp-token");
  },

  /**
   * 清理用户信息
   */
  clearUserInfo(){
    localStorage.removeItem("wvp-user");
    localStorage.removeItem("wvp-token");
  },
  /**
   * 更新token
   * @param header
   */
  setToken(token) {
    localStorage.setItem("wvp-token", token);
  }
}
