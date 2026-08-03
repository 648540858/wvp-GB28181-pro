import { nextTick } from 'vue'
import { mount } from '@vue/test-utils'
import { createStore } from 'vuex'
import Navbar from '@/layout/components/Navbar.vue'
import antCompat from '@/components/antCompat'

describe('Navbar', () => {
  it('opens the user dropdown and handles password changes', async() => {
    const openDialog = jest.fn()
    const store = createStore({
      getters: {
        sidebar: () => ({ opened: true }),
        name: () => 'admin'
      }
    })

    const wrapper = mount(Navbar, {
      attachTo: document.body,
      global: {
        plugins: [store, antCompat],
        stubs: {
          Breadcrumb: true,
          Hamburger: true,
          ChangePasswordDialog: {
            name: 'ChangePasswordDialog',
            methods: { openDialog },
            template: '<div />'
          },
          'svg-icon': true
        }
      }
    })

    await nextTick()
    expect(wrapper.find('button.avatar-wrapper').exists()).toBe(true)
    expect(wrapper.findComponent({ name: 'ElDropdown' }).exists()).toBe(true)
    await wrapper.find('button.avatar-wrapper').trigger('click')
    await nextTick()
    const changePassword = Array.from(document.body.querySelectorAll('.ant-dropdown-menu-item'))
      .find(item => item.textContent.includes('修改密码'))
    expect(changePassword).toBeDefined()
    changePassword.dispatchEvent(new MouseEvent('click', { bubbles: true }))
    await nextTick()
    expect(openDialog).toHaveBeenCalledTimes(1)
    wrapper.unmount()
  })
})
