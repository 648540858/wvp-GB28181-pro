import { nextTick } from 'vue'
import { mount } from '@vue/test-utils'
import antCompat from '@/components/antCompat'
import { dialogText, locale } from '@/components/ui/config'
import MenuItem from '@/layout/components/Sidebar/Item.vue'

window.matchMedia = window.matchMedia || jest.fn().mockImplementation(query => ({
  matches: false,
  media: query,
  addListener: jest.fn(),
  removeListener: jest.fn(),
  addEventListener: jest.fn(),
  removeEventListener: jest.fn()
}))

const mountWithCompat = component => mount(component, {
  global: { plugins: [antCompat] }
})

describe('Ant Design compatibility components', () => {
  it('keeps all built-in dialog actions in Simplified Chinese', () => {
    expect(locale.locale).toBe('zh-cn')
    expect(locale.Modal).toEqual(expect.objectContaining({
      okText: '确定',
      cancelText: '取消',
      justOkText: '知道了'
    }))
    expect(locale.Popconfirm).toEqual(expect.objectContaining({
      okText: '确定',
      cancelText: '取消'
    }))
    expect(dialogText.closeText).toBe('关闭')
  })

  it('preserves tab labels, keys and v-model updates', async() => {
    const wrapper = mountWithCompat({
      data: () => ({ active: 'first' }),
      template: `
        <el-tabs v-model="active">
          <el-tab-pane label="First" name="first">First content</el-tab-pane>
          <el-tab-pane label="Second" name="second">Second content</el-tab-pane>
        </el-tabs>
      `
    })

    expect(wrapper.findAll('.ant-tabs-tab').map(tab => tab.text())).toEqual(['First', 'Second'])
    await wrapper.findAll('.ant-tabs-tab')[1].trigger('click')
    expect(wrapper.vm.active).toBe('second')
    expect(wrapper.text()).toContain('Second content')
  })

  it('keeps unnamed conditional tab panes on unique keys', async() => {
    const wrapper = mountWithCompat({
      data: () => ({ showSecond: true }),
      template: `
        <el-tabs>
          <el-tab-pane label="First">First content</el-tab-pane>
          <el-tab-pane v-if="showSecond" label="Second">Second content</el-tab-pane>
        </el-tabs>
      `
    })

    await wrapper.findAll('.ant-tabs-tab')[1].trigger('click')
    await nextTick()

    const activePanes = wrapper.findAll('.ant-tabs-tabpane-active')
    expect(activePanes).toHaveLength(1)
    expect(activePanes[0].text()).toContain('Second content')
  })

  it('emits the legacy menu select value', async() => {
    const wrapper = mountWithCompat({
      data: () => ({ selected: '' }),
      template: `
        <el-menu @select="selected = $event">
          <el-menu-item index="camera">Camera</el-menu-item>
        </el-menu>
      `
    })

    await wrapper.find('.ant-menu-item').trigger('click')
    expect(wrapper.vm.selected).toBe('camera')
  })

  it('maps table selection and presentation props', () => {
    const wrapper = mountWithCompat({
      data: () => ({ rows: [{ id: 1, name: 'One', enabled: true }, { id: 2, name: 'Two', enabled: false }] }),
      methods: {
        selectable(row) {
          return row.enabled
        }
      },
      template: `
        <el-table :data="rows" row-key="id" header-row-class-name="table-header" empty-text="No rows">
          <el-table-column type="selection" :selectable="selectable" reserve-selection />
          <el-table-column prop="name" label="Name" />
        </el-table>
      `
    })

    expect(wrapper.find('thead tr').classes()).toContain('table-header')
    const rowCheckboxes = wrapper.findAll('tbody input[type="checkbox"]')
    expect(rowCheckboxes).toHaveLength(2)
    expect(rowCheckboxes[0].attributes('disabled')).toBeUndefined()
    expect(rowCheckboxes[1].attributes('disabled')).toBeDefined()
  })

  it('keeps operation columns on one line', () => {
    const wrapper = mountWithCompat({
      data: () => ({ rows: [{ id: 1 }] }),
      template: `
        <el-table :data="rows" row-key="id">
          <el-table-column label="操作">
            <template #default>
              <el-button type="text">编辑</el-button>
              <el-divider direction="vertical" />
              <el-button type="text">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      `
    })

    const actions = wrapper.get('td.ant-compat-table-actions')
    expect(actions.find('.ant-compat-table-action-list').exists()).toBe(true)
    expect(actions.findAll('.ant-btn')).toHaveLength(2)
    expect(actions.find('.ant-divider').exists()).toBe(false)
    expect(wrapper.find('td.ant-table-cell-fix-right').exists()).toBe(true)
  })

  it('keeps only the current nested menu item selected', async() => {
    const wrapper = mountWithCompat({
      data: () => ({ active: '/device' }),
      template: `
        <el-menu :default-active="active" mode="vertical">
          <el-submenu index="/device">
            <template #title>设备接入</template>
            <el-menu-item index="/device">国标设备</el-menu-item>
            <el-menu-item index="/device/jtDevice">部标设备</el-menu-item>
            <el-menu-item index="/device/push">推流列表</el-menu-item>
            <el-menu-item index="/device/proxy">拉流代理</el-menu-item>
          </el-submenu>
        </el-menu>
      `
    })

    await wrapper.find('.ant-menu-submenu-title').trigger('click')
    await nextTick()
    expect(wrapper.findAll('.ant-menu-item')).toHaveLength(4)
    expect(wrapper.findAll('.ant-menu-item-selected')).toHaveLength(1)
    expect(wrapper.find('.ant-menu-item-selected').text()).toContain('国标设备')

    await wrapper.setData({ active: '/device/jtDevice' })
    await nextTick()
    expect(wrapper.findAll('.ant-menu-item-selected')).toHaveLength(1)
    expect(wrapper.find('.ant-menu-item-selected').text()).toContain('部标设备')
  })

  it('keeps sidebar icons in the native Ant icon area when collapsed', () => {
    const wrapper = mount({
      components: { MenuItem },
      template: `
        <el-menu collapse mode="vertical">
          <el-menu-item index="/dashboard">
            <menu-item icon="dashboard" title="控制台" />
          </el-menu-item>
          <el-submenu index="/device">
            <template #title>
              <menu-item icon="devices" title="设备接入" />
            </template>
            <el-menu-item index="/device">国标设备</el-menu-item>
          </el-submenu>
        </el-menu>
      `
    }, {
      global: {
        plugins: [antCompat],
        stubs: {
          'svg-icon': {
            props: ['iconClass'],
            template: '<svg class="svg-icon" :data-icon="iconClass" />'
          }
        }
      }
    })

    expect(wrapper.find('.ant-menu-inline-collapsed').exists()).toBe(true)
    expect(wrapper.find('.ant-menu-item > .ant-menu-item-icon[data-icon="dashboard"]').exists()).toBe(true)
    expect(wrapper.find('.ant-menu-submenu-title > .ant-menu-item-icon[data-icon="devices"]').exists()).toBe(true)
  })

  it('centers local and fullscreen loading overlays in their intended area', async() => {
    const wrapper = mountWithCompat({
      data: () => ({ loading: true }),
      template: '<div v-loading="loading" element-loading-text="局部加载中"><span>Content</span></div>'
    })

    const localOverlay = wrapper.get('.ant-compat-loading')
    expect(localOverlay.classes()).not.toContain('ant-compat-loading--fullscreen')
    expect(localOverlay.element.parentElement).toBe(wrapper.element)
    expect(localOverlay.text()).toContain('局部加载中')

    const loading = wrapper.vm.$loading({
      lock: true,
      text: '页面加载中',
      background: 'rgba(255, 255, 255, 0.9)'
    })
    await nextTick()

    const fullscreenOverlay = document.body.querySelector('.ant-compat-loading--fullscreen')
    expect(fullscreenOverlay).not.toBeNull()
    expect(fullscreenOverlay.style.display).toBe('flex')
    expect(fullscreenOverlay.textContent).toContain('页面加载中')
    expect(document.body.classList.contains('ant-compat-loading-locked')).toBe(true)

    loading.close()
    expect(fullscreenOverlay.style.display).toBe('none')
    expect(document.body.classList.contains('ant-compat-loading-locked')).toBe(false)
    wrapper.unmount()
  })

  it('forwards dropdown item clicks and legacy commands', async() => {
    const itemClicked = jest.fn()
    const wrapper = mount({
      data: () => ({ command: '' }),
      methods: { itemClicked },
      template: `
        <el-dropdown trigger="click" @command="command = $event">
          <button type="button">更多</button>
          <el-dropdown-menu slot="dropdown">
            <el-dropdown-item command="edit" @click="itemClicked">编辑</el-dropdown-item>
          </el-dropdown-menu>
        </el-dropdown>
      `
    }, {
      attachTo: document.body,
      global: { plugins: [antCompat] }
    })

    await wrapper.find('button').trigger('click')
    await nextTick()
    const item = document.body.querySelector('.ant-dropdown-menu-item')
    expect(item).not.toBeNull()
    item.dispatchEvent(new MouseEvent('click', { bubbles: true }))
    await nextTick()

    expect(wrapper.vm.command).toBe('edit')
    expect(itemClicked).toHaveBeenCalledTimes(1)
    wrapper.unmount()
  })

  it('supports dropdown menus without the legacy slot attribute', async() => {
    const command = jest.fn()
    const wrapper = mount({
      data: () => ({ speed: [4, 2] }),
      methods: { command },
      template: `
        <el-dropdown @command="command">
          <button type="button">更多</button>
          <el-dropdown-menu>
            <el-dropdown-item :command="speed">2X</el-dropdown-item>
          </el-dropdown-menu>
        </el-dropdown>
      `
    }, {
      attachTo: document.body,
      global: { plugins: [antCompat] }
    })

    await wrapper.find('button').trigger('click')
    await nextTick()
    const item = document.body.querySelector('.ant-dropdown-menu-item')
    expect(item).not.toBeNull()
    item.dispatchEvent(new MouseEvent('click', { bubbles: true }))
    await nextTick()

    expect(command).toHaveBeenCalledWith([4, 2])
    wrapper.unmount()
  })

  it('adapts callback form validators to promise validation', async() => {
    const validator = jest.fn((rule, value, callback) => {
      callback(value ? undefined : new Error('Name is required'))
    })
    const wrapper = mountWithCompat({
      data: () => ({
        form: { name: '' },
        rules: { name: [{ validator }] }
      }),
      template: `
        <el-form ref="formRef" :model="form" :rules="rules">
          <el-form-item prop="name">
            <el-input v-model="form.name" />
          </el-form-item>
        </el-form>
      `
    })

    await expect(wrapper.vm.$refs.formRef.validate()).rejects.toBeDefined()
    await wrapper.find('input').setValue('camera')
    await expect(wrapper.vm.$refs.formRef.validate()).resolves.toBeDefined()
    expect(validator.mock.calls.map(call => call[1])).toEqual(expect.arrayContaining(['', 'camera']))
  })

  it('forwards native input type changes', async() => {
    const wrapper = mountWithCompat({
      data: () => ({ passwordType: 'password', password: '' }),
      template: '<el-input v-model="password" :type="passwordType" />'
    })

    expect(wrapper.find('input').attributes('type')).toBe('password')
    await wrapper.setData({ passwordType: 'text' })
    expect(wrapper.find('input').attributes('type')).toBe('text')
  })

})
