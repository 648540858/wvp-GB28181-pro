import { flushPromises, mount } from '@vue/test-utils'
import antCompat from '@/components/antCompat'
import request from '@/utils/request'
import HistoryLog from '@/views/operations/historyLog.vue'
import RealLog from '@/views/operations/realLog.vue'
import ShowLog from '@/views/operations/showLog.vue'

jest.mock('@/utils/request', () => jest.fn())

window.matchMedia = window.matchMedia || jest.fn().mockImplementation(query => ({
  matches: false,
  media: query,
  addListener: jest.fn(),
  removeListener: jest.fn(),
  addEventListener: jest.fn(),
  removeEventListener: jest.fn()
}))

const store = {
  getters: {
    token: 'test-access-token'
  }
}

const mountShowLog = props => mount(ShowLog, {
  props,
  global: {
    plugins: [antCompat],
    mocks: { $store: store }
  }
})

describe('operations log views', () => {
  let wrapper
  let originalWebSocket

  beforeEach(() => {
    request.mockReset()
    originalWebSocket = global.WebSocket
  })

  afterEach(() => {
    if (wrapper) {
      wrapper.unmount()
      wrapper = null
    }
    global.WebSocket = originalWebSocket
  })

  it('renders the history log response and encodes the selected file name', async() => {
    request.mockResolvedValue('第一行日志\n第二行日志')
    wrapper = mountShowLog({ fileUrl: '/api/log/file/wvp.log' })

    await flushPromises()

    expect(request).toHaveBeenCalledWith({
      method: 'get',
      url: '/api/log/file/wvp.log',
      responseType: 'text'
    })
    expect(wrapper.vm.logData).toContain('第一行日志')
    expect(wrapper.get('.log-viewer').text()).toContain('第二行日志')

    const page = {}
    HistoryLog.methods.showLogView.call(page, { fileName: 'wvp history #1.log' })
    expect(page.fileUrl).toBe('/api/log/file/wvp%20history%20%231.log')
    expect(page.showLog).toBe(true)
  })

  it('updates the realtime viewer when websocket messages are pushed', async() => {
    const sockets = []
    global.WebSocket = class WebSocketMock {
      constructor(url, protocol) {
        this.url = url
        this.protocol = protocol
        sockets.push(this)
      }

      close() {
        this.closed = true
      }
    }

    wrapper = mountShowLog({ remoteUrl: 'ws://localhost/dev-api/channel/log' })
    expect(sockets).toHaveLength(1)
    expect(sockets[0].protocol).toBe('test-access-token')

    sockets[0].onopen()
    sockets[0].onmessage({ data: '实时日志第一行' })
    await wrapper.vm.$nextTick()

    expect(wrapper.vm.connectionState).toBe('connected')
    expect(wrapper.vm.logData).toContain('实时日志第一行')
    expect(wrapper.get('.log-viewer').text()).toContain('实时日志第一行')
  })

  it('builds the Vite development websocket URL through the API proxy', () => {
    const nodeEnv = process.env.NODE_ENV
    const baseApi = process.env.VUE_APP_BASE_API
    try {
      process.env.NODE_ENV = 'development'
      process.env.VUE_APP_BASE_API = '/dev-api'

      expect(RealLog.methods.getUrl()).toBe('ws://localhost/dev-api/channel/log')
    } finally {
      process.env.NODE_ENV = nodeEnv
      process.env.VUE_APP_BASE_API = baseApi
    }
  })
})
