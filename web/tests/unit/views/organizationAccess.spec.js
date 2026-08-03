import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import antCompat from '@/components/antCompat'
import AppMain from '@/layout/components/AppMain.vue'
import Group from '@/views/channel/group/index.vue'
import Region from '@/views/channel/region/index.vue'
import UnusualGroupChannelSelect from '@/views/channel/group/UnusualGroupChannelSelect.vue'
import UnusualRegionChannelSelect from '@/views/channel/region/UnusualRegionChannelSelect.vue'
import GbChannelSelect from '@/views/dialog/GbChannelSelect.vue'
import GbDeviceSelect from '@/views/dialog/GbDeviceSelect.vue'

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
  if ([
    'commonChanel/getCivilCodeList',
    'commonChanel/getParentList',
    'commonChanel/getUnusualCivilCodeList',
    'commonChanel/getUnusualParentList',
    'device/queryDevices'
  ].includes(type)) {
    return Promise.resolve(emptyList)
  }
  return Promise.resolve([])
})

const store = {
  dispatch,
  getters: {
    authority: 0
  }
}

const stubs = {
  GbChannelSelect: true,
  UnusualRegionChannelSelect: true,
  UnusualGroupChannelSelect: true,
  regionEdit: true,
  groupEdit: true,
  gbDeviceSelect: true
}

describe('organization pages', () => {
  beforeEach(() => {
    dispatch.mockClear()
  })

  it.each([
    ['行政区划', Region, 'commonChanel/getCivilCodeList'],
    ['业务分组', Group, 'commonChanel/getParentList']
  ])('mounts the %s page without reading DOM refs during created', async(_name, component, action) => {
    const error = jest.spyOn(console, 'error').mockImplementation(() => undefined)
    const wrapper = mount(component, {
      global: {
        plugins: [antCompat],
        mocks: { $store: store, $channelTypeList: {} },
        stubs
      }
    })

    try {
      await flushPromises()
      expect(dispatch).toHaveBeenCalledWith(action, expect.anything())
      expect(wrapper.find('.ant-table').exists()).toBe(true)
      expect(error.mock.calls.flat().join(' ')).not.toMatch(/TypeError|Unhandled error/)
    } finally {
      wrapper.unmount()
      error.mockRestore()
    }
  })

  it.each([
    ['行政区划', Region],
    ['业务分组', Group]
  ])('keeps the %s table height within the page viewport', async(_name, component) => {
    const wrapper = mount(component, {
      global: {
        plugins: [antCompat],
        mocks: { $store: store, $channelTypeList: {} },
        stubs
      }
    })
    await flushPromises()

    const queryForm = wrapper.vm.$refs.queryForm.$el
    const pagination = wrapper.vm.$refs.pagination.$el
    const tableContainer = queryForm.parentElement
    const tableHeader = wrapper.find('.ant-table-thead').element

    Object.defineProperty(wrapper.element, 'clientHeight', { configurable: true, value: 720 })
    Object.defineProperty(tableContainer, 'clientHeight', { configurable: true, value: 5000 })
    Object.defineProperty(queryForm, 'offsetHeight', { configurable: true, value: 96 })
    Object.defineProperty(pagination, 'offsetHeight', { configurable: true, value: 48 })
    Object.defineProperty(tableHeader, 'offsetHeight', { configurable: true, value: 48 })
    wrapper.element.style.paddingTop = '20px'
    wrapper.element.style.paddingBottom = '20px'
    queryForm.style.marginTop = '0px'
    queryForm.style.marginBottom = '16px'
    pagination.style.marginTop = '0px'
    pagination.style.marginBottom = '0px'

    wrapper.vm.updateTableHeight()
    await wrapper.vm.$nextTick()

    expect(wrapper.vm.tableHeight).toBe(472)
    wrapper.unmount()
  })

  it('does not load the device selector before its dialog opens', async() => {
    const wrapper = mount(GbDeviceSelect, {
      global: {
        plugins: [antCompat],
        mocks: { $store: store, $channelTypeList: {} }
      }
    })

    await flushPromises()

    expect(dispatch).not.toHaveBeenCalledWith('device/queryDevices', expect.anything())
    wrapper.unmount()
  })

  it.each([
    ['添加通道', GbChannelSelect, { dataType: 'civilCode' }, 'commonChanel/getCivilCodeList', '.channel-select-dialog-content'],
    ['选择设备', GbDeviceSelect, {}, 'device/queryDevices', '.device-select-dialog-content'],
    ['行政区划异常通道', UnusualRegionChannelSelect, {}, 'commonChanel/getUnusualCivilCodeList', '.unusual-channel-dialog-content'],
    ['业务分组异常通道', UnusualGroupChannelSelect, {}, 'commonChanel/getUnusualParentList', '.unusual-channel-dialog-content']
  ])('centers the %s loading overlay inside the visible dialog content', async(_name, component, props, action, selector) => {
    const wrapper = mount(component, {
      props,
      attachTo: document.body,
      global: {
        plugins: [antCompat],
        mocks: { $store: store, $channelTypeList: {} }
      }
    })

    try {
      wrapper.vm.openDialog(jest.fn())
      await flushPromises()

      const content = document.body.querySelector(selector)
      expect(content).not.toBeNull()
      const overlay = Array.from(content.children).find(child => child.classList.contains('ant-compat-loading'))
      expect(dispatch).toHaveBeenCalledWith(action, expect.anything())
      expect(overlay).toBeDefined()
      expect(overlay.parentElement).toBe(content)
    } finally {
      wrapper.unmount()
    }
  })

  it('keeps both pages mounted while navigating repeatedly inside AppMain', async() => {
    const error = jest.spyOn(console, 'error').mockImplementation(() => undefined)
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: '/commonChannel/region', name: 'Region', component: () => Promise.resolve(Region) },
        { path: '/commonChannel/group', name: 'Group', component: () => Promise.resolve(Group) }
      ]
    })
    await router.push('/commonChannel/region')
    await router.isReady()

    const wrapper = mount(AppMain, {
      global: {
        plugins: [router, antCompat],
        mocks: { $store: store, $channelTypeList: {} },
        stubs
      }
    })

    try {
      for (const path of [
        '/commonChannel/group',
        '/commonChannel/region',
        '/commonChannel/group',
        '/commonChannel/region'
      ]) {
        await router.push(path)
        await flushPromises()
        expect(wrapper.find('.ant-table').exists()).toBe(true)
      }
      expect(error.mock.calls.flat().join(' ')).not.toMatch(/parentNode|TypeError|Unhandled error/)
    } finally {
      wrapper.unmount()
      error.mockRestore()
    }
  })
})
