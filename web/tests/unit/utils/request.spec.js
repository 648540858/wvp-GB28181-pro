import { Message, MessageBoxConfirm } from '@/components/antCompat'
import request from '@/utils/request'
import { setToken } from '@/utils/auth'
import store from '@/store'

jest.mock('@/store', () => ({
  getters: {
    token: '',
    showConfirmBoxForLoginLose: true
  },
  commit: jest.fn(),
  dispatch: jest.fn()
}))

jest.mock('@/utils/auth', () => ({
  getToken: jest.fn(),
  setToken: jest.fn()
}))

jest.mock('@/components/antCompat', () => ({
  MessageBoxConfirm: jest.fn(),
  Message: {
    error: jest.fn(),
    warning: jest.fn()
  }
}))

describe('request response errors', () => {
  afterEach(() => {
    jest.clearAllMocks()
  })

  it('rejects a failed login request without showing the expired-session dialog', async() => {
    const error = new Error('Request failed with status code 401')
    error.config = { url: '/api/user/login' }
    error.response = {
      status: 401,
      data: { msg: '用户名或密码错误' }
    }

    const promise = request.get('/api/user/login', {
      adapter: () => Promise.reject(error)
    })

    await expect(promise).rejects.toBe(error)
    expect(MessageBoxConfirm).not.toHaveBeenCalled()
  })

  it('preserves network errors that do not have a response', async() => {
    const error = new Error('Network Error')
    error.config = { url: '/api/user/login' }

    const promise = request.get('/api/user/login', {
      adapter: () => Promise.reject(error)
    })

    await expect(promise).rejects.toBe(error)
    expect(Message.error).not.toHaveBeenCalled()
  })

  it('stores a renewed access token returned by the server', async() => {
    const response = await request.put('/api/server/security/config', {}, {
      adapter: config => Promise.resolve({
        data: { code: 0, data: {} },
        status: 200,
        statusText: 'OK',
        headers: { 'access-token': 'renewed-token' },
        config
      })
    })

    expect(response.code).toBe(0)
    expect(store.commit).toHaveBeenCalledWith('user/SET_TOKEN', 'renewed-token')
    expect(setToken).toHaveBeenCalledWith('renewed-token')
  })
})
