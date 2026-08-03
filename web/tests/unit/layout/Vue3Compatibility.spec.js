import { shallowMount } from '@vue/test-utils'
import Item from '@/layout/components/Sidebar/Item.vue'
import StreamMediaPanel from '@/views/common/streamMediaPanel.vue'

describe('Vue 3 UI compatibility', () => {
  it('renders the sidebar icon and title', () => {
    const wrapper = shallowMount(Item, {
      props: {
        icon: 'el-icon-video-camera',
        title: '设备'
      },
      global: {
        stubs: {
          'ant-icon': {
            props: ['name'],
            template: '<i :class="name" />'
          },
          'svg-icon': true
        }
      }
    })

    expect(wrapper.find('i.el-icon-video-camera').exists()).toBe(true)
    expect(wrapper.find('span').text()).toBe('设备')
  })

  it('binds the read-only stream URLs to the inputs', () => {
    const wrapper = shallowMount(StreamMediaPanel, {
      props: {
        playerUrl: 'http://example.com/player',
        playUrl: 'http://example.com/live.flv'
      },
      global: {
        stubs: {
          'el-form': { template: '<form><slot /></form>' },
          'el-form-item': { template: '<div><slot /></div>' },
          'el-button': true,
          'el-dropdown': true,
          'el-dropdown-item': true,
          'el-dropdown-menu': true,
          'el-tag': true,
          'ant-icon': true,
          'el-input': {
            inheritAttrs: false,
            props: ['value', 'modelValue'],
            template: '<input :value="modelValue ?? value" disabled>'
          }
        }
      }
    })

    expect(wrapper.findAll('input').map(input => input.element.value)).toEqual([
      'http://example.com/player',
      '<iframe src="http://example.com/player"></iframe>',
      'http://example.com/live.flv'
    ])
  })
})
