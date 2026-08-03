import { defineComponent } from 'vue'
import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import AppMain from '@/layout/components/AppMain.vue'

describe('AppMain', () => {
  it('renders route pages without depending on the tags view cache', async() => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: '/device/jtDevice', component: { template: '<div data-testid="jt-device-page">部标设备</div>' } }
      ]
    })
    await router.push('/device/jtDevice')
    await router.isReady()

    const wrapper = mount(AppMain, {
      global: { plugins: [router] }
    })

    expect(wrapper.get('[data-testid="jt-device-page"]').text()).toBe('部标设备')
  })

  it('lets RouterView manage the matched component lifecycle directly', () => {
    const RouterViewStub = defineComponent({
      setup(_, { slots }) {
        return {
          hasDefaultSlot: Boolean(slots.default)
        }
      },
      template: '<div data-testid="router-view" :data-has-default-slot="String(hasDefaultSlot)" />'
    })
    const wrapper = mount(AppMain, {
      global: {
        stubs: { RouterView: RouterViewStub }
      }
    })

    expect(wrapper.get('[data-testid="router-view"]').attributes('data-has-default-slot')).toBe('false')
  })

  it('renders Vue Router 4 route components inside AppMain', async() => {
    const warn = jest.spyOn(console, 'warn').mockImplementation(() => undefined)
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: '/', component: { template: '<div>首页</div>' } },
        { path: '/device', component: { name: 'DevicePage', template: '<div data-testid="device-page">设备页面</div>' } }
      ]
    })
    await router.push('/device')
    await router.isReady()

    const wrapper = mount(AppMain, {
      global: {
        plugins: [router],
        mocks: {
          $store: { state: { tagsView: { cachedViews: [] } } }
        }
      }
    })

    expect(wrapper.find('[data-testid="device-page"]').exists()).toBe(true)
    expect(warn.mock.calls.flat().join(' ')).not.toContain('can no longer be used directly')
    warn.mockRestore()
  })

  it('switches every device access page without a router-view update error', async() => {
    const error = jest.spyOn(console, 'error').mockImplementation(() => undefined)
    const pages = [
      ['/device', 'DevicePage', '国标设备'],
      ['/device/jtDevice', 'JTDevicePage', '部标设备'],
      ['/device/push', 'PushPage', '推流列表'],
      ['/device/proxy', 'ProxyPage', '拉流代理']
    ]
    const router = createRouter({
      history: createMemoryHistory(),
      routes: pages.map(([path, name, title]) => ({
        path,
        name,
        component: { name, template: `<div data-testid="route-page">${title}</div>` }
      }))
    })
    await router.push('/device')
    await router.isReady()

    const wrapper = mount(AppMain, {
      global: {
        plugins: [router],
        mocks: {
          $store: { state: { tagsView: { cachedViews: pages.map(([, name]) => name) } } }
        }
      }
    })

    try {
      const navigations = []
      for (const [path] of pages.slice(1)) {
        navigations.push(router.push(path))
        await flushPromises()
      }
      await Promise.allSettled(navigations)
      await flushPromises()
      expect(wrapper.get('[data-testid="route-page"]').text()).toBe('拉流代理')
      expect(error.mock.calls.flat().join(' ')).not.toMatch(/parentNode|Unhandled error during execution of component update/)
    } finally {
      wrapper.unmount()
      error.mockRestore()
    }
  })
})
