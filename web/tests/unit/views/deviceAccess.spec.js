import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import antCompat from '@/components/antCompat'
import AppMain from '@/layout/components/AppMain.vue'
import Device from '@/views/device/index.vue'
import DeviceList from '@/views/device/list.vue'
import JTDevice from '@/views/jtDevice/index.vue'
import JTDeviceList from '@/views/jtDevice/list.vue'
import StreamPush from '@/views/streamPush/index.vue'
import StreamProxy from '@/views/streamProxy/index.vue'

window.matchMedia = window.matchMedia || jest.fn().mockImplementation(query => ({
  matches: false,
  media: query,
  addListener: jest.fn(),
  removeListener: jest.fn(),
  addEventListener: jest.fn(),
  removeEventListener: jest.fn()
}))

const emptyList = { total: 0, list: [] }

const dispatch = jest.fn(type => {
  if (['device/queryDevices', 'jtDevice/queryDevices', 'streamPush/queryList', 'streamProxy/queryList'].includes(type)) {
    return Promise.resolve(emptyList)
  }
  return Promise.resolve([])
})

const store = {
  dispatch,
  state: {
    tagsView: {
      cachedViews: ['Device', 'JTDevice', 'PushList', 'Proxy']
    }
  },
  getters: {
    serverId: 'test-server',
    token: 'test-token'
  }
}

const stubs = {
  deviceEdit: true,
  syncChannelProgress: true,
  configInfo: true,
  timeStatistics: true,
  attribute: true,
  position: true,
  textMsg: true,
  telephoneCallback: true,
  driverInfo: true,
  connectionServer: true,
  controlDoor: true,
  mediaAttribute: true,
  phoneBook: true,
  queryMediaList: true,
  shootingNow: true,
  streamPushPlayer: true,
  addStreamTOGB: true,
  importChannel: true,
  StreamPushEdit: true,
  buildPushStreamUrl: true,
  streamProxyPlayer: true,
  StreamProxyEdit: true
}

describe('device access pages', () => {
  beforeEach(() => {
    jest.useFakeTimers()
    dispatch.mockClear()
  })

  afterEach(() => {
    jest.useRealTimers()
  })

  it.each([
    ['国标设备', DeviceList, 'device/queryDevices'],
    ['部标设备', JTDeviceList, 'jtDevice/queryDevices'],
    ['推流列表', StreamPush, 'streamPush/queryList'],
    ['拉流代理', StreamProxy, 'streamProxy/queryList']
  ])('renders the %s page with an empty data response', async(_name, component, action) => {
    const error = jest.spyOn(console, 'error').mockImplementation(() => undefined)
    const wrapper = mount(component, {
      global: {
        plugins: [antCompat],
        mocks: { $store: store },
        stubs
      }
    })

    await flushPromises()
    expect(dispatch).toHaveBeenCalledWith(action, expect.anything())
    expect(wrapper.find('.ant-table').exists()).toBe(true)
    expect(wrapper.find('.ant-empty').exists()).toBe(true)
    expect(error.mock.calls.flat().join(' ')).not.toContain('should be `Select.Option`')
    wrapper.unmount()
    error.mockRestore()
  })

  it('renders the complete 部标设备 page with its real dialog components', async() => {
    const error = jest.spyOn(console, 'error').mockImplementation(() => undefined)
    const wrapper = mount(JTDevice, {
      global: {
        plugins: [antCompat],
        mocks: { $store: store }
      }
    })

    try {
      await flushPromises()
      expect(wrapper.find('#JTDevice').exists()).toBe(true)
      expect(wrapper.find('.ant-table').exists()).toBe(true)
      expect(error.mock.calls.flat().join(' ')).not.toMatch(/Unhandled error|TypeError/)
    } finally {
      wrapper.unmount()
      error.mockRestore()
    }
  })

  it('renders all four pages while navigating inside AppMain', async() => {
    const error = jest.spyOn(console, 'error').mockImplementation(() => undefined)
    const routes = [
      ['/device', 'Device', Device, 'device/queryDevices'],
      ['/device/jtDevice', 'JTDevice', JTDevice, 'jtDevice/queryDevices'],
      ['/device/push', 'PushList', StreamPush, 'streamPush/queryList'],
      ['/device/proxy', 'Proxy', StreamProxy, 'streamProxy/queryList']
    ]
    const router = createRouter({
      history: createMemoryHistory(),
      routes: routes.map(([path, name, component]) => ({
        path,
        name,
        component: () => Promise.resolve(component)
      }))
    })
    dispatch.mockClear()
    await router.push('/device')
    await router.isReady()

    const wrapper = mount(AppMain, {
      global: {
        plugins: [router, antCompat],
        mocks: { $store: store },
        stubs
      }
    })

    try {
      const navigationOrder = [
        ...routes,
        routes[0],
        routes[3],
        routes[1],
        routes[2]
      ]
      for (const [index, [path, , , action]] of navigationOrder.entries()) {
        if (index > 0) await router.push(path)
        await flushPromises()
        expect(dispatch).toHaveBeenCalledWith(action, expect.anything())
        expect(wrapper.find('.ant-table').exists()).toBe(true)
      }
      expect(error.mock.calls.flat().join(' ')).not.toMatch(/parentNode|Unhandled error during execution of component update/)
    } finally {
      wrapper.unmount()
      error.mockRestore()
    }
  })
})
