import { mount } from '@vue/test-utils'
import antCompat from '@/components/antCompat'
import Login from '@/views/login/index.vue'

window.matchMedia = window.matchMedia || jest.fn().mockImplementation(query => ({
  matches: false,
  media: query,
  addListener: jest.fn(),
  removeListener: jest.fn(),
  addEventListener: jest.fn(),
  removeEventListener: jest.fn()
}))

const flushPromises = () => new Promise(resolve => setTimeout(resolve, 0))

describe('login page', () => {
  it('shows validation errors without submitting an empty form', async() => {
    const dispatch = jest.fn()
    const message = jest.fn()
    const wrapper = mount(Login, {
      global: {
        plugins: [antCompat],
        mocks: {
          $route: { query: {} },
          $router: { push: jest.fn() },
          $store: { dispatch },
          $message: message
        },
        stubs: { SvgIcon: true }
      }
    })

    await wrapper.find('.login-button').trigger('click')
    await flushPromises()

    expect(message).toHaveBeenCalledWith({
      showClose: true,
      message: '请输入用户名和密码',
      type: 'warning'
    })
    expect(dispatch).not.toHaveBeenCalled()
    wrapper.unmount()
  })

  it('submits credentials when the login button is clicked', async() => {
    const dispatch = jest.fn().mockResolvedValue()
    const push = jest.fn()
    const wrapper = mount(Login, {
      global: {
        plugins: [antCompat],
        mocks: {
          $route: { query: {} },
          $router: { push },
          $store: { dispatch }
        },
        stubs: { SvgIcon: true }
      }
    })

    await wrapper.find('#login-username').setValue('admin')
    await wrapper.find('#login-password').setValue('password')
    await wrapper.find('.login-button').trigger('click')
    await flushPromises()

    expect(dispatch).toHaveBeenCalledWith('user/login', {
      username: 'admin',
      password: 'password'
    })
    expect(push).toHaveBeenCalledWith({ path: '/' })
    wrapper.unmount()
  })
})
