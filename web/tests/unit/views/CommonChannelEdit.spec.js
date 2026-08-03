import { flushPromises, shallowMount } from '@vue/test-utils'
import { reactive } from 'vue'
import antCompat from '@/components/antCompat'
import CommonChannelEdit from '@/views/common/CommonChannelEdit.vue'

describe('CommonChannelEdit', () => {
  it('loads a reactive data form without calling structuredClone', async() => {
    const originalStructuredClone = window.structuredClone
    window.structuredClone = jest.fn(() => {
      throw new DOMException('The object could not be cloned.', 'DataCloneError')
    })
    const dataForm = reactive({
      gbId: 'proxy-1',
      gbName: '拉流代理',
      gbDeviceId: '34020000001320000001'
    })
    const dispatch = jest.fn(() => Promise.resolve([]))
    const error = jest.spyOn(console, 'error').mockImplementation(() => undefined)

    try {
      const wrapper = shallowMount(CommonChannelEdit, {
        props: { dataForm },
        global: {
          plugins: [antCompat],
          mocks: {
            $store: { dispatch }
          }
        }
      })

      await flushPromises()

      expect(wrapper.vm.form).toMatchObject({
        gbId: 'proxy-1',
        gbName: '拉流代理',
        gbDeviceId: '34020000001320000001'
      })
      expect(window.structuredClone).not.toHaveBeenCalled()
      expect(error.mock.calls.flat().join(' ')).not.toMatch(/DataCloneError|Unhandled error/)
      wrapper.unmount()
    } finally {
      window.structuredClone = originalStructuredClone
      error.mockRestore()
    }
  })

  it('loads edit data without assigning to the dataForm prop', async() => {
    const dataForm = { gbId: 'old-id', gbName: '旧名称' }
    const loaded = {
      gbId: 'channel-1',
      gbName: '测试通道',
      gbDeviceId: '34020000001320000001',
      enableBroadcast: 1
    }
    const dispatch = jest.fn((type) => {
      if (type === 'commonChanel/queryOne') return Promise.resolve({ ...loaded })
      return Promise.resolve([])
    })
    const message = jest.fn()
    message.success = jest.fn()

    const wrapper = shallowMount(CommonChannelEdit, {
      props: { id: 'channel-1', dataForm },
      global: {
        plugins: [antCompat],
        mocks: {
          $store: { dispatch },
          $message: message
        }
      }
    })

    await flushPromises()

    expect(wrapper.vm.form).toMatchObject(loaded)
    expect(wrapper.vm.form.enableBroadcastForBool).toBe(true)
    expect(wrapper.vm.originalForm).toMatchObject(loaded)
    expect(dataForm).toEqual({ gbId: 'old-id', gbName: '旧名称' })
    expect(message).not.toHaveBeenCalled()
  })
})
