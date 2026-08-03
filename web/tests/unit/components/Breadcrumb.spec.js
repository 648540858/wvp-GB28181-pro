import { mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import Breadcrumb from '@/components/Breadcrumb/index.vue'
import antCompat from '@/components/antCompat'

const routes = [
  {
    path: '/',
    name: 'home',
    children: [{ path: 'dashboard', name: 'dashboard', component: { template: '<div />' } }]
  },
  {
    path: '/menu',
    name: 'menu',
    children: [{
      path: 'menu1',
      name: 'menu1',
      meta: { title: 'menu1' },
      children: [{
        path: 'menu1-1',
        name: 'menu1-1',
        meta: { title: 'menu1-1' },
        component: { template: '<div />' }
      }, {
        path: 'menu1-2',
        name: 'menu1-2',
        redirect: 'noredirect',
        meta: { title: 'menu1-2' },
        children: [{
          path: 'menu1-2-1',
          name: 'menu1-2-1',
          meta: { title: 'menu1-2-1' },
          component: { template: '<div />' }
        }, {
          path: 'menu1-2-2',
          name: 'menu1-2-2',
          component: { template: '<div />' }
        }]
      }]
    }]
  }
]

const router = createRouter({ history: createMemoryHistory(), routes })
const wrapper = mount(Breadcrumb, { global: { plugins: [router, antCompat] } })

describe('Breadcrumb.vue', () => {
  it.each([
    ['/dashboard', 1],
    ['/menu/menu1', 2],
    ['/menu/menu1/menu1-2/menu1-2-1', 4],
    ['/menu/menu1/menu1-2/menu1-2-2', 3]
  ])('renders route %s', async(path, count) => {
    await router.push(path)
    await wrapper.vm.$nextTick()
    expect(wrapper.findAll('.ant-breadcrumb-link')).toHaveLength(count)
  })

  it('renders the last breadcrumb as plain text', async() => {
    await router.push('/menu/menu1/menu1-2/menu1-2-1')
    await wrapper.vm.$nextTick()
    const breadcrumbs = wrapper.findAll('.ant-breadcrumb-link')
    expect(breadcrumbs.at(3).find('a').exists()).toBe(false)
  })
})
