import DeviceChannel from '@/views/device/channel/index.vue'
import JTChannelEdit from '@/views/jtDevice/channel/edit.vue'
import QueryMediaList from '@/views/jtDevice/dialog/queryMediaList.vue'
import PlatformEdit from '@/views/platform/edit.vue'
import StreamPushEdit from '@/views/streamPush/edit.vue'

describe('Vue 3 prop safety', () => {
  it.each([
    ['部标通道', JTChannelEdit, 'jtChannel'],
    ['上级平台', PlatformEdit, 'platform'],
    ['推流', StreamPushEdit, 'streamPush']
  ])('creates an independent local form for %s editing', (_name, component, propName) => {
    const source = { id: 1, name: '原始名称' }
    const state = component.data.call({ [propName]: source })

    expect(state.form).toEqual(source)
    expect(state.form).not.toBe(source)
    state.form.name = '修改后的名称'
    expect(source.name).toBe('原始名称')
  })

  it('loads a device subdirectory through local state instead of route props', () => {
    const context = {
      activeParentChannelId: null,
      currentPage: 3,
      searchStr: 'camera',
      channelType: 'true',
      online: 'true',
      initData: jest.fn()
    }

    DeviceChannel.methods.changeSubchannel.call(context, { deviceId: 'sub-channel-1' })

    expect(context.activeParentChannelId).toBe('sub-channel-1')
    expect(context.currentPage).toBe(1)
    expect(context.searchStr).toBe('')
    expect(context.initData).toHaveBeenCalledTimes(1)
  })

  it('does not clear a parent-owned channel list when media search closes', () => {
    const channelList = [{ channelId: '1' }]
    const context = {
      channelList,
      mediaDataInfoList: [{ id: 1 }],
      type: 2,
      chanelId: 1,
      event: 3
    }

    QueryMediaList.methods.close.call(context)

    expect(context.channelList).toBe(channelList)
    expect(context.mediaDataInfoList).toEqual([])
  })
})
