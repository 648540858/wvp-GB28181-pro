import Record from '@/views/channel/record.vue'

jest.mock('@/views/common/playerTabs.vue', () => ({}))
jest.mock('@/views/common/VideoTimeLine/index.vue', () => ({}))
jest.mock('@/views/dialog/recordDownload.vue', () => ({}))
jest.mock('@/views/dialog/chooseTimeRange.vue', () => ({}))
jest.mock('screenfull', () => ({}))

/** 使用可控的停止响应验证页面请求归属，避免旧请求完成时销毁新的播放器。 */
function fixture() {
  let resolve
  const pending = new Promise(done => { resolve = done })
  const vm = {
    streamInfo: { stream: 'player-a' },
    videoUrl: 'playing',
    $refs: { recordVideoPlayer: { pause: jest.fn(), destroy: jest.fn() } },
    $store: { dispatch: jest.fn(() => pending) },
    $message: { error: jest.fn() }
  }
  vm.stopPlayRecord = callback => Record.methods.stopPlayRecord.call(vm, callback)
  return { vm, resolve }
}

describe('录像停止的会话归属', () => {
  it('停止按钮先停止自己的后端流，再销毁播放器', async() => {
    const { vm, resolve } = fixture()
    const stopped = Record.methods.stopPLay.call(vm)
    expect(vm.$store.dispatch).toHaveBeenCalledWith('commonChanel/stopPlayback', { channelId: undefined, stream: 'player-a' })
    expect(vm.$refs.recordVideoPlayer.destroy).not.toHaveBeenCalled()
    resolve()
    await stopped
    expect(vm.$refs.recordVideoPlayer.destroy).toHaveBeenCalledTimes(1)
    expect(vm.streamInfo).toBe(null)
  })

  it('旧停止响应不能清除或销毁期间建立的新会话', async() => {
    const { vm, resolve } = fixture()
    const stopped = Record.methods.stopPLay.call(vm)
    vm.streamInfo = { stream: 'player-b' }
    resolve()
    await stopped
    expect(vm.streamInfo.stream).toBe('player-b')
    expect(vm.$refs.recordVideoPlayer.destroy).not.toHaveBeenCalled()
  })

  it('同区间重播使用相同流名时，迟到停止也不能销毁新一轮播放器', async() => {
    const { vm, resolve } = fixture()
    const firstStop = Record.methods.stopPLay.call(vm)
    let resolveSecond
    const secondPending = new Promise(done => { resolveSecond = done })
    vm.$store.dispatch.mockReturnValueOnce(secondPending)
    const replay = { stream: 'player-a' }
    const secondStop = vm.stopPlayRecord(() => { vm.streamInfo = replay })

    resolveSecond()
    await secondStop
    expect(vm.streamInfo).toBe(replay)
    resolve()
    await firstStop

    expect(vm.streamInfo).toBe(replay)
    expect(vm.$refs.recordVideoPlayer.destroy).not.toHaveBeenCalled()
  })

  it('卸载事件对象不会被错误地作为结束函数调用', async() => {
    const { vm, resolve } = fixture()
    const stopped = vm.stopPlayRecord({ type: 'beforeunload' })
    resolve()
    await stopped
    expect(vm.$message.error).not.toHaveBeenCalled()
    expect(vm.streamInfo).toBe(null)
  })
})
