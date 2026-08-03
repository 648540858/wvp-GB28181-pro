import { mount } from '@vue/test-utils'
import antCompat from '@/components/antCompat'
import SecurityConfig from '@/views/operations/securityConfig.vue'
import { getSecurityConfig, updateSecurityConfig } from '@/api/server'

jest.mock('@/api/server', () => ({
  getSecurityConfig: jest.fn(),
  updateSecurityConfig: jest.fn()
}))

window.matchMedia = jest.fn().mockImplementation(query => ({
  matches: false,
  media: query,
  onchange: null,
  addListener: jest.fn(),
  removeListener: jest.fn(),
  addEventListener: jest.fn(),
  removeEventListener: jest.fn(),
  dispatchEvent: jest.fn()
}))

const flushPromises = () => new Promise(resolve => setTimeout(resolve, 0))

const securityConfig = {
  interfaceAuthentication: true,
  interfaceAuthenticationExcludes: ['/api/user/login'],
  loginTimeoutMinutes: 120,
  jwtExpirationMinutes: 120,
  accessTokenHeader: 'access-token',
  apiKeyHeader: 'X-API-KEY',
  passwordEncoder: 'BCrypt',
  sessionCreationPolicy: 'ALWAYS',
  csrfEnabled: false,
  contentTypeOptionsEnabled: false,
  allowedOrigins: [],
  allowAllOrigins: true,
  docEnabled: true,
  httpsEnabled: false,
  serverPort: 18080,
  jwkFile: 'config/jwk.json',
  sipCacheServerConnections: false,
  jsonLibrary: 'Fastjson2',
  jsonLibraryVersion: '2.0.62',
  fastjson1Present: false,
  fastjsonRiskStatus: '未检测到 Fastjson 1.x，当前运行时使用 Fastjson2',
  warnings: ['HTTPS 未启用']
}

function findValueCell(wrapper, label) {
  const labelCell = wrapper.findAll('th').find(cell => cell.text() === label)
  return labelCell.element.nextElementSibling
}

function findTag(wrapper, text) {
  return wrapper.findAll('.ant-tag').find(tag => tag.text() === text)
}

describe('securityConfig.vue', () => {
  let wrapper
  const message = { error: jest.fn() }

  afterEach(() => {
    if (wrapper) {
      wrapper.unmount()
      wrapper = null
    }
    jest.clearAllMocks()
  })

  it('uses security-aware Ant Design tag colors', async() => {
    getSecurityConfig.mockResolvedValue(securityConfig)
    wrapper = mount(SecurityConfig, {
      global: {
        plugins: [antCompat],
        mocks: { $message: message }
      }
    })
    await flushPromises()
    await wrapper.vm.$nextTick()

    expect(wrapper.find('.ant-switch-checked').exists()).toBe(true)
    expect(findTag(wrapper, '全部来源').classes()).toContain('ant-tag-orange')
    expect(findValueCell(wrapper, 'Fastjson 1.x').querySelector('.ant-tag-green').textContent.trim()).toBe('未检测到')
    expect(wrapper.text()).not.toContain('undefined 分钟')
  })

  it('saves editable settings and applies returned runtime data', async() => {
    getSecurityConfig.mockResolvedValue(securityConfig)
    updateSecurityConfig.mockResolvedValue({
      ...securityConfig,
      interfaceAuthentication: false,
      loginTimeoutMinutes: 0
    })
    wrapper = mount(SecurityConfig, {
      global: {
        plugins: [antCompat],
        mocks: { $message: { error: jest.fn(), success: jest.fn() } }
      }
    })
    await flushPromises()

    wrapper.vm.form.interfaceAuthentication = false
    wrapper.vm.form.loginTimeoutMinutes = 0
    wrapper.vm.saveConfig()
    await flushPromises()

    expect(updateSecurityConfig).toHaveBeenCalledWith(expect.objectContaining({
      interfaceAuthentication: false,
      loginTimeoutMinutes: 0
    }))
    expect(wrapper.vm.config.interfaceAuthentication).toBe(false)
  })

  it('restores the saved timeout from the legacy response field', async() => {
    const legacyConfig = { ...securityConfig, jwtExpirationMinutes: 45 }
    delete legacyConfig.loginTimeoutMinutes
    getSecurityConfig.mockResolvedValue(legacyConfig)
    updateSecurityConfig.mockResolvedValue(legacyConfig)
    wrapper = mount(SecurityConfig, {
      global: {
        plugins: [antCompat],
        mocks: { $message: { error: jest.fn(), success: jest.fn() } }
      }
    })
    await flushPromises()

    expect(wrapper.vm.form.loginTimeoutMinutes).toBe(45)
    wrapper.vm.form.docEnabled = false
    wrapper.vm.saveConfig()
    await flushPromises()

    expect(updateSecurityConfig).toHaveBeenCalledWith(expect.objectContaining({
      loginTimeoutMinutes: 45,
      docEnabled: false
    }))
  })

  it('keeps zero as a valid saved timeout', async() => {
    getSecurityConfig.mockResolvedValue({ ...securityConfig, loginTimeoutMinutes: 0 })
    wrapper = mount(SecurityConfig, {
      global: {
        plugins: [antCompat],
        mocks: { $message: message }
      }
    })
    await flushPromises()

    expect(wrapper.vm.form.loginTimeoutMinutes).toBe(0)
    expect(wrapper.vm.buildRequest().loginTimeoutMinutes).toBe(0)
  })

  it('uses the current timeout when an old response omits timeout fields', async() => {
    const legacyConfig = { ...securityConfig }
    delete legacyConfig.loginTimeoutMinutes
    delete legacyConfig.jwtExpirationMinutes
    getSecurityConfig.mockResolvedValue(legacyConfig)
    updateSecurityConfig.mockResolvedValue(legacyConfig)
    wrapper = mount(SecurityConfig, {
      global: {
        plugins: [antCompat],
        mocks: { $message: { error: jest.fn(), success: jest.fn() } }
      }
    })
    await flushPromises()

    expect(wrapper.vm.form.loginTimeoutMinutes).toBe(120)
    wrapper.vm.form.contentTypeOptionsEnabled = true
    wrapper.vm.saveConfig()
    await flushPromises()

    expect(updateSecurityConfig).toHaveBeenCalledWith(expect.objectContaining({
      loginTimeoutMinutes: 120,
      contentTypeOptionsEnabled: true
    }))
  })

  it('shows a retry state when loading fails', async() => {
    getSecurityConfig.mockRejectedValue(new Error('network unavailable'))
    wrapper = mount(SecurityConfig, {
      global: {
        plugins: [antCompat],
        mocks: { $message: message }
      }
    })
    await flushPromises()
    await wrapper.vm.$nextTick()

    expect(wrapper.find('.ant-empty-description').text()).toBe('安全配置加载失败')
    expect(wrapper.find('.ant-empty button').text()).toBe('重新加载')
    expect(message.error).toHaveBeenCalledWith('network unavailable')
  })
})
