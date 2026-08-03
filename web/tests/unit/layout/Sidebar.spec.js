import { h } from 'vue'
import { flushPromises, mount, shallowMount } from '@vue/test-utils'
import { createStore } from 'vuex'
import { createMemoryHistory, createRouter } from 'vue-router'
import { readFileSync } from 'fs'
import { compileString } from 'sass'
import path from 'path'
import Sidebar from '@/layout/components/Sidebar/index.vue'
import { constantRoutes } from '@/router'
import antCompat from '@/components/antCompat'

jest.mock('@/styles/variables.module.scss', () => ({
  menuBg: '#304156',
  menuText: '#bfcbd9',
  menuActiveText: '#409EFF'
}))

const ElMenuStub = {
  name: 'ElMenu',
  props: ['backgroundColor', 'textColor', 'activeTextColor'],
  render() {
    return h('nav', {
      'data-background-color': this.backgroundColor,
      'data-text-color': this.textColor,
      'data-active-text-color': this.activeTextColor
    }, this.$slots.default?.())
  }
}

function navigationRoutes(routes) {
  return routes.map(route => ({
    ...route,
    component: route.component ? { render: () => h('div') } : undefined,
    children: route.children ? navigationRoutes(route.children) : undefined
  }))
}

describe('Sidebar', () => {
  it('passes the configured theme colors to the Ant menu adapter', () => {
    const store = createStore({
      state: { settings: { sidebarLogo: false } },
      getters: { sidebar: () => ({ opened: true }) }
    })
    const wrapper = shallowMount(Sidebar, {
      global: {
        plugins: [store],
        mocks: {
          $route: { meta: {}, path: '/' },
          $router: { options: { routes: [] } }
        },
        stubs: {
          'el-scrollbar': { template: '<div><slot /></div>' },
          'el-menu': ElMenuStub,
          Logo: true,
          SidebarItem: true
        }
      }
    })

    const menu = wrapper.find('nav')
    expect(menu.attributes('data-background-color')).toBe('#304156')
    expect(menu.attributes('data-text-color')).toBe('#bfcbd9')
    expect(menu.attributes('data-active-text-color')).toBe('#409EFF')
  })

  it('keeps long Ant menus scrollable without visible scrollbars', () => {
    const stylesDir = path.resolve(__dirname, '../../../src/styles')
    const variables = readFileSync(path.join(stylesDir, 'variables.scss'), 'utf8')
    const sidebar = readFileSync(path.join(stylesDir, 'sidebar.scss'), 'utf8')
      .replace('@use \'./variables\' as *;', '')
    const css = compileString(`${variables}\n${sidebar}`).css

    expect(css).toMatch(/\.scrollbar-wrapper[^{]*\{[^}]*overflow-y:\s*auto\s*!important[^}]*scrollbar-width:\s*none/s)
    expect(css).toMatch(/\.ant-menu-submenu-popup \.ant-menu[^{]*\{[^}]*overflow-y:\s*auto[^}]*scrollbar-width:\s*none/s)
    expect(css).toMatch(/\.ant-menu-item-selected[^{]*\{[^}]*background:\s*rgba\(37,\s*99,\s*235,\s*0\.18\)\s*!important[^}]*box-shadow:[^}]*#3b82f6/s)
    expect(css).toMatch(/\.ant-menu-inline-collapsed[^{]*\.ant-menu-item[^{]*\{[^}]*justify-content:\s*center/s)
    expect(css).toMatch(/\.ant-menu-inline-collapsed[^{]*\.svg-icon[^{]*\{[^}]*margin:\s*0\s*!important/s)
  })

  it('keeps all device access pages under the device menu', () => {
    const deviceRoute = constantRoutes.find(route => route.path === '/device')
    const visiblePaths = deviceRoute.children
      .filter(route => !route.hidden)
      .map(route => route.path)

    expect(visiblePaths).toEqual(['', 'jtDevice', 'push', 'proxy'])
  })

  it('updates the selected item after navigating between device pages', async() => {
    const router = createRouter({ history: createMemoryHistory(), routes: constantRoutes })
    await router.push('/device')
    await router.isReady()
    const store = createStore({
      state: { settings: { sidebarLogo: false } },
      getters: { sidebar: () => ({ opened: true }) }
    })
    const wrapper = mount(Sidebar, {
      global: {
        plugins: [router, store, antCompat],
        stubs: {
          'svg-icon': true,
          Logo: true
        }
      }
    })

    const deviceTitle = wrapper.findAll('.ant-menu-submenu-title')
      .find(title => title.text().includes('设备接入'))
    expect(deviceTitle).toBeDefined()
    await deviceTitle.trigger('click')
    expect(wrapper.findAll('.ant-menu-item').filter(item => item.text().includes('部标设备'))).toHaveLength(1)
    expect(wrapper.findAll('.ant-menu-item').filter(item => item.text().includes('推流列表'))).toHaveLength(1)
    expect(wrapper.findAll('.ant-menu-item').filter(item => item.text().includes('拉流代理'))).toHaveLength(1)

    await router.push('/device/jtDevice')
    await wrapper.vm.$nextTick()
    expect(wrapper.findAll('.ant-menu-item-selected')).toHaveLength(1)
    expect(wrapper.find('.ant-menu-item-selected').text()).toContain('部标设备')

    await router.push('/jtDevice')
    await wrapper.vm.$nextTick()
    expect(wrapper.findAll('.ant-menu-item-selected')).toHaveLength(1)
    expect(wrapper.find('.ant-menu-item-selected').text()).toContain('部标设备')
    wrapper.unmount()
  })

  it('navigates when every visible menu item is clicked', async() => {
    const router = createRouter({ history: createMemoryHistory(), routes: navigationRoutes(constantRoutes) })
    await router.push('/dashboard')
    await router.isReady()
    const store = createStore({
      state: { settings: { sidebarLogo: false } },
      getters: { sidebar: () => ({ opened: true }) }
    })
    const wrapper = mount(Sidebar, {
      global: {
        plugins: [router, store, antCompat],
        stubs: {
          'svg-icon': true,
          Logo: true
        }
      }
    })

    for (const title of ['设备接入', '组织结构', '运维中心']) {
      const submenu = wrapper.findAll('.ant-menu-submenu-title').find(item => item.text().includes(title))
      expect(submenu).toBeDefined()
      await submenu.trigger('click')
    }

    const targets = [
      ['控制台', '/dashboard'],
      ['分屏监控', '/live'],
      ['通道列表', '/channel'],
      ['电子地图', '/map'],
      ['国标设备', '/device'],
      ['部标设备', '/device/jtDevice'],
      ['推流列表', '/device/push'],
      ['拉流代理', '/device/proxy'],
      ['行政区划', '/commonChannel/region'],
      ['业务分组', '/commonChannel/group'],
      ['报警管理', '/alarm'],
      ['录制计划', '/recordPlan'],
      ['云端录像', '/cloudRecord'],
      ['媒体节点', '/mediaServer'],
      ['国标级联', '/platform'],
      ['用户管理', '/user'],
      ['平台信息', '/operations/systemInfo'],
      ['安全配置', '/operations/securityConfig'],
      ['历史日志', '/operations/historyLog'],
      ['实时日志', '/operations/realLog']
    ]

    for (const [title, routePath] of targets) {
      const menuItem = wrapper.findAll('.ant-menu-item').find(item => item.text().includes(title))
      expect(menuItem).toBeDefined()
      await menuItem.trigger('click')
      await flushPromises()
      expect(router.currentRoute.value.path).toBe(routePath)
    }
    wrapper.unmount()
  })
})
