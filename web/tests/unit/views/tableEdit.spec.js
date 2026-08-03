import { flushPromises, mount } from '@vue/test-utils'
import antCompat from '@/components/antCompat'
import ChannelList from '@/views/channel/index.vue'
import ChannelPtzCruiseConfig from '@/views/channel/common/ptzCruiseConfig.vue'
import DevicePtzCruiseConfig from '@/views/device/common/ptzCruiseConfig.vue'
import DeviceChannelList from '@/views/device/channel/index.vue'
import DeviceList from '@/views/device/list.vue'
import JTDeviceChannelList from '@/views/jtDevice/channel/index.vue'
import JTDeviceList from '@/views/jtDevice/list.vue'
import Platform from '@/views/platform/index.vue'
import RecordPlan from '@/views/recordPlan/index.vue'
import StreamProxy from '@/views/streamProxy/index.vue'
import StreamPush from '@/views/streamPush/index.vue'
import UserList from '@/views/user/index.vue'

window.matchMedia = window.matchMedia || jest.fn().mockImplementation(query => ({
  matches: false,
  media: query,
  addListener: jest.fn(),
  removeListener: jest.fn(),
  addEventListener: jest.fn(),
  removeEventListener: jest.fn()
}))

const channel = {
  gbId: 'channel-1',
  gbDeviceId: '34020000001320000001',
  gbName: '测试通道',
  gbStatus: 'ON',
  dataType: 1,
  ptzType: 0,
  ptzTypeText: '未知',
  enableBroadcast: 1
}

const editButton = wrapper => wrapper.findAll('button').find(button => button.text() === '编辑')

describe('table edit actions', () => {
  beforeEach(() => {
    jest.useFakeTimers()
  })

  afterEach(() => {
    jest.useRealTimers()
  })

  it('opens the channel editor from the operation column without blanking the page', async() => {
    const dispatch = jest.fn(type => {
      if (type === 'commonChanel/getList') return Promise.resolve({ total: 1, list: [{ ...channel }] })
      if (type === 'commonChanel/queryOne') return Promise.resolve({ ...channel })
      return Promise.resolve([])
    })
    const error = jest.spyOn(console, 'error').mockImplementation(() => undefined)
    const wrapper = mount(ChannelList, {
      global: {
        plugins: [antCompat],
        mocks: {
          $store: {
            dispatch,
            getters: { authority: 0 }
          },
          $channelTypeList: {
            1: { id: 1, name: '国标设备', style: {} }
          }
        },
        stubs: {
          devicePlayer: true,
          audioTalk: true,
          ptzConfig: true,
          chooseCivilCode: true,
          chooseGroup: true,
          channelCode: true,
          resetChannel: true
        }
      }
    })

    try {
      await flushPromises()
      const button = editButton(wrapper)
      expect(button).toBeDefined()

      await button.trigger('click')
      await flushPromises()

      expect(wrapper.find('#CommonChannelEdit').exists()).toBe(true)
      expect(wrapper.text()).toContain('编辑通道')
      expect(wrapper.find('input').element.value).toBe('测试通道')
      expect(error.mock.calls.flat().join(' ')).not.toMatch(/parentNode|Unhandled error|TypeError/)
    } finally {
      wrapper.unmount()
      error.mockRestore()
    }
  })

  it('opens the stream proxy editor with the selected row data', async() => {
    const streamProxy = {
      id: 1,
      app: 'live',
      stream: 'camera-1',
      srcUrl: 'rtsp://127.0.0.1/live',
      type: 'default',
      timeout: 10,
      noneReader: 1,
      enableDisableNoneReader: true,
      enable: true,
      enableAudio: true,
      playLoading: false,
      pulling: false
    }
    const dispatch = jest.fn(type => {
      if (type === 'streamProxy/queryList') return Promise.resolve({ total: 1, list: [{ ...streamProxy }] })
      return Promise.resolve([])
    })
    const error = jest.spyOn(console, 'error').mockImplementation(() => undefined)
    const wrapper = mount(StreamProxy, {
      global: {
        plugins: [antCompat],
        mocks: {
          $store: {
            dispatch,
            getters: { serverId: 'test-server', token: 'test-token' }
          },
          $router: { push: jest.fn() }
        },
        stubs: { streamProxyPlayer: true }
      }
    })

    try {
      await flushPromises()
      const button = editButton(wrapper)
      expect(button).toBeDefined()

      await button.trigger('click')
      await flushPromises()

      expect(wrapper.find('#StreamProxyEdit').exists()).toBe(true)
      expect(wrapper.findAll('input').some(input => input.element.value === 'live')).toBe(true)
      expect(wrapper.find('.edit-header').exists()).toBe(true)
      expect(wrapper.findAll('.proxy-section')).toHaveLength(2)
      expect(wrapper.text()).toContain('接入信息')
      expect(wrapper.text()).toContain('运行策略')
      const tabs = wrapper.findAll('.ant-tabs-tab')
      expect(tabs).toHaveLength(2)
      await tabs[1].trigger('click')
      await flushPromises()
      expect(wrapper.findAll('.ant-tabs-tabpane-active')).toHaveLength(1)
      expect(wrapper.find('.ant-tabs-tabpane-active').text()).toContain('基础标识')
      expect(wrapper.find('.ant-tabs-tabpane-active').text()).toContain('联网与安全')
      expect(wrapper.find('.ant-tabs-tabpane-active').text()).toContain('能力与属性')
      expect(error.mock.calls.flat().join(' ')).not.toMatch(/Unhandled error|TypeError/)
    } finally {
      wrapper.unmount()
      error.mockRestore()
    }
  })

  it('opens the stream push editor with the selected row data', async() => {
    const row = { id: 2, app: 'push', stream: 'camera-2', gbName: '推流通道', pushing: false }
    const dispatch = jest.fn(type => {
      if (type === 'streamPush/queryList') return Promise.resolve({ total: 1, list: [{ ...row }] })
      return Promise.resolve([])
    })
    const error = jest.spyOn(console, 'error').mockImplementation(() => undefined)
    const wrapper = mount(StreamPush, {
      global: {
        plugins: [antCompat],
        mocks: {
          $store: { dispatch, getters: { serverId: 'test-server' } },
          $router: { push: jest.fn() }
        },
        stubs: {
          streamPushPlayer: true,
          addStreamTOGB: true,
          importChannel: true,
          buildPushStreamUrl: true,
          channelCode: true,
          resetChannel: true,
          chooseCivilCode: true,
          chooseGroup: true
        }
      }
    })

    try {
      await flushPromises()
      await editButton(wrapper).trigger('click')
      await flushPromises()

      expect(wrapper.find('#ChannelEdit').exists()).toBe(true)
      expect(wrapper.findAll('input').some(input => input.element.value === 'push')).toBe(true)
      expect(error.mock.calls.flat().join(' ')).not.toMatch(/Unhandled error|TypeError/)
    } finally {
      wrapper.unmount()
      error.mockRestore()
    }
  })

  it('opens the platform editor with the selected row data', async() => {
    const row = { id: 3, name: '上级平台', serverGBId: '34020000002000000001', enable: true }
    const dispatch = jest.fn(type => {
      if (type === 'platform/query') return Promise.resolve({ total: 1, list: [{ ...row }] })
      if (type === 'platform/getServerConfig') return Promise.resolve({ deviceIp: '127.0.0.1' })
      return Promise.resolve([])
    })
    const error = jest.spyOn(console, 'error').mockImplementation(() => undefined)
    const wrapper = mount(Platform, {
      global: {
        plugins: [antCompat],
        mocks: { $store: { dispatch, getters: { serverId: 'test-server' } } },
        stubs: { shareChannel: true }
      }
    })

    try {
      await flushPromises()
      await editButton(wrapper).trigger('click')
      await flushPromises()

      expect(wrapper.find('#PlatformEdit').exists()).toBe(true)
      expect(wrapper.findAll('input').some(input => input.element.value === '上级平台')).toBe(true)
      expect(error.mock.calls.flat().join(' ')).not.toMatch(/Unhandled error|TypeError/)
    } finally {
      wrapper.unmount()
      error.mockRestore()
    }
  })

  it('opens the GB device edit dialog from the operation column', async() => {
    const row = { deviceId: '34020000001110000001', name: '国标设备', online: 1 }
    const dispatch = jest.fn(type => {
      if (type === 'device/queryDevices') return Promise.resolve({ total: 1, list: [{ ...row }] })
      return Promise.resolve([])
    })
    const error = jest.spyOn(console, 'error').mockImplementation(() => undefined)
    const wrapper = mount(DeviceList, {
      global: {
        plugins: [antCompat],
        mocks: { $store: { dispatch, getters: { serverId: 'test-server' } } },
        stubs: { syncChannelProgress: true, configInfo: true, timeStatistics: true }
      }
    })

    try {
      await flushPromises()
      await editButton(wrapper).trigger('click')
      await flushPromises()

      const editor = wrapper.findComponent({ name: 'DeviceEdit' })
      expect(editor.vm.showDialog).toBe(true)
      expect(editor.vm.form.deviceId).toBe(row.deviceId)
      expect(error.mock.calls.flat().join(' ')).not.toMatch(/Unhandled error|TypeError/)
    } finally {
      wrapper.unmount()
      error.mockRestore()
    }
  })

  it('opens the JT device edit dialog from the operation column', async() => {
    const row = { id: 4, phoneNumber: '13800138000', name: '部标设备' }
    const dispatch = jest.fn(type => {
      if (type === 'jtDevice/queryDevices') return Promise.resolve({ total: 1, list: [{ ...row }] })
      return Promise.resolve([])
    })
    const error = jest.spyOn(console, 'error').mockImplementation(() => undefined)
    const wrapper = mount(JTDeviceList, {
      global: {
        plugins: [antCompat],
        mocks: { $store: { dispatch, getters: {} } },
        stubs: {
          configInfo: true,
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
          shootingNow: true
        }
      }
    })

    try {
      await flushPromises()
      await editButton(wrapper).trigger('click')
      await flushPromises()

      const editor = wrapper.findComponent({ name: 'DeviceEdit' })
      expect(editor.vm.showDialog).toBe(true)
      expect(editor.vm.form.phoneNumber).toBe(row.phoneNumber)
      expect(error.mock.calls.flat().join(' ')).not.toMatch(/Unhandled error|TypeError/)
    } finally {
      wrapper.unmount()
      error.mockRestore()
    }
  })

  it('opens the record plan edit dialog from the operation column', async() => {
    const row = { id: 5, name: '全天录像' }
    const dispatch = jest.fn(type => {
      if (type === 'recordPlan/queryList') return Promise.resolve({ total: 1, list: [{ ...row }] })
      if (type === 'recordPlan/getPlan') return Promise.resolve({ planItemList: [] })
      return Promise.resolve([])
    })
    const error = jest.spyOn(console, 'error').mockImplementation(() => undefined)
    const wrapper = mount(RecordPlan, {
      global: {
        plugins: [antCompat],
        mocks: { $store: { dispatch, getters: {} } },
        stubs: { LinkChannelRecord: true, weekTimePicker: true }
      }
    })

    try {
      await flushPromises()
      await editButton(wrapper).trigger('click')
      await flushPromises()

      const editor = wrapper.findComponent({ name: 'EditRecordPlan' })
      expect(editor.vm.showDialog).toBe(true)
      expect(editor.vm.planName).toBe(row.name)
      expect(error.mock.calls.flat().join(' ')).not.toMatch(/Unhandled error|TypeError/)
    } finally {
      wrapper.unmount()
      error.mockRestore()
    }
  })

  it('opens the GB device channel editor from the operation column', async() => {
    const row = { id: 'channel-db-1', deviceId: 'channel-code-1', name: '设备通道', ptzType: 0 }
    const dispatch = jest.fn(type => {
      if (type === 'device/queryDeviceOne') return Promise.resolve({ deviceId: 'device-1', online: 1 })
      if (type === 'device/queryChannels') return Promise.resolve({ total: 1, list: [{ ...row }] })
      if (type === 'commonChanel/queryOne') return Promise.resolve({ ...channel, gbId: row.id })
      return Promise.resolve([])
    })
    const error = jest.spyOn(console, 'error').mockImplementation(() => undefined)
    const wrapper = mount(DeviceChannelList, {
      props: { deviceId: 'device-1' },
      global: {
        plugins: [antCompat],
        mocks: { $store: { dispatch, getters: {} } },
        stubs: {
          devicePlayer: true,
          audioTalk: true,
          ptzConfig: true,
          cameraConfig: true,
          channelCode: true,
          resetChannel: true,
          chooseCivilCode: true,
          chooseGroup: true
        }
      }
    })

    try {
      await flushPromises()
      await editButton(wrapper).trigger('click')
      await flushPromises()

      expect(wrapper.find('#CommonChannelEdit').exists()).toBe(true)
      expect(wrapper.findAll('input').some(input => input.element.value === '测试通道')).toBe(true)
      expect(error.mock.calls.flat().join(' ')).not.toMatch(/Unhandled error|TypeError/)
    } finally {
      wrapper.unmount()
      error.mockRestore()
    }
  })

  it('opens the JT device channel editor from the operation column', async() => {
    const row = { id: 6, channelId: '1', name: '部标通道', gbId: null }
    const dispatch = jest.fn(type => {
      if (type === 'jtDevice/queryDeviceById') return Promise.resolve({ id: 'jt-1', phoneNumber: '13800138000', status: true })
      if (type === 'jtDevice/queryChannels') return Promise.resolve({ total: 1, list: [{ ...row }] })
      return Promise.resolve([])
    })
    const error = jest.spyOn(console, 'error').mockImplementation(() => undefined)
    const wrapper = mount(JTDeviceChannelList, {
      props: { deviceId: 'jt-1' },
      global: {
        plugins: [antCompat],
        mocks: { $store: { dispatch, getters: {} } },
        stubs: {
          devicePlayer: true,
          channelCode: true,
          resetChannel: true,
          chooseCivilCode: true,
          chooseGroup: true
        }
      }
    })

    try {
      await flushPromises()
      await editButton(wrapper).trigger('click')
      await flushPromises()

      expect(wrapper.find('#channelEdit').exists()).toBe(true)
      expect(wrapper.findAll('input').some(input => input.element.value === '部标通道')).toBe(true)
      expect(error.mock.calls.flat().join(' ')).not.toMatch(/Unhandled error|TypeError/)
    } finally {
      wrapper.unmount()
      error.mockRestore()
    }
  })

  it.each([
    ['common channel', ChannelPtzCruiseConfig, { channelId: 'channel-1' }],
    ['device channel', DevicePtzCruiseConfig, { deviceId: 'device-1', channelDeviceId: 'channel-1' }]
  ])('opens the %s cruise editor from the operation column', async(_name, component, props) => {
    const dispatch = jest.fn(() => Promise.resolve([]))
    const error = jest.spyOn(console, 'error').mockImplementation(() => undefined)
    const wrapper = mount(component, {
      props,
      global: {
        plugins: [antCompat],
        mocks: { $store: { dispatch, getters: {} } }
      }
    })

    try {
      await flushPromises()
      await wrapper.setData({
        cruiseTours: [{
          id: 1,
          name: '巡航组1',
          presets: [{ presetId: 1, dwellTime: 15, speed: 7 }]
        }]
      })
      await editButton(wrapper).trigger('click')
      await flushPromises()

      expect(wrapper.vm.formVisible).toBe(true)
      expect(wrapper.vm.formName).toBe('巡航组1')
      expect(error.mock.calls.flat().join(' ')).not.toMatch(/Unhandled error|TypeError/)
    } finally {
      wrapper.unmount()
      error.mockRestore()
    }
  })

  it('opens the user password editor from the operation column', async() => {
    const row = { id: 7, username: 'admin', role: { name: '管理员' } }
    const dispatch = jest.fn(type => {
      if (type === 'user/queryList') return Promise.resolve({ total: 1, list: [{ ...row }] })
      return Promise.resolve([])
    })
    const error = jest.spyOn(console, 'error').mockImplementation(() => undefined)
    const wrapper = mount(UserList, {
      global: {
        plugins: [antCompat],
        mocks: { $store: { dispatch, getters: {} } },
        stubs: { changePushKey: true, addUser: true, apiKeyManager: true }
      }
    })

    try {
      await flushPromises()
      const button = wrapper.findAll('button').find(item => item.text() === '修改密码')
      await button.trigger('click')
      await flushPromises()

      const editor = wrapper.findComponent({ name: 'ChangePasswordForAdmin' })
      expect(editor.vm.showDialog).toBe(true)
      expect(editor.vm.form.id).toBe(row.id)
      expect(error.mock.calls.flat().join(' ')).not.toMatch(/Unhandled error|TypeError/)
    } finally {
      wrapper.unmount()
      error.mockRestore()
    }
  })
})
