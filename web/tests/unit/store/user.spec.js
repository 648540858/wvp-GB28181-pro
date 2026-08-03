import user from '@/store/modules/user'
import { login } from '@/api/user'

jest.mock('@/api/user', () => ({
  add: jest.fn(),
  changePassword: jest.fn(),
  changePasswordForAdmin: jest.fn(),
  changePushKey: jest.fn(),
  getUserInfo: jest.fn(),
  login: jest.fn(),
  logout: jest.fn(),
  queryList: jest.fn(),
  removeById: jest.fn()
}))

jest.mock('@/utils/auth', () => ({
  getToken: jest.fn(),
  setToken: jest.fn(),
  setName: jest.fn(),
  removeToken: jest.fn(),
  removeName: jest.fn(),
  setServerId: jest.fn(),
  removeServerId: jest.fn()
}))

jest.mock('@/router', () => ({
  resetRouter: jest.fn()
}))

describe('user store login', () => {
  it('rejects an empty login response with a readable error', async() => {
    login.mockResolvedValue(undefined)

    await expect(user.actions.login({ commit: jest.fn() }, {
      username: 'admin',
      password: 'password'
    })).rejects.toThrow('登录响应数据异常')
  })
})
