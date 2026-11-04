import request from '@/utils/request'
import { playback } from '@/api/commonChannel'
import { play } from '@/api/playback'

jest.mock('@/utils/request', () => jest.fn(() => Promise.resolve({})))
describe('默认共享、显式独立的回放请求身份', () => {
  beforeEach(() => request.mockClear())

  it('通用回放默认不注入会话身份，允许两个读者复用同一设备回放', () => {
    const params = { channelId: 5, startTime: '2026-10-23 20:40:00', endTime: '2026-10-23 20:41:00' }
    playback(params)
    playback(params)
    request.mock.calls.forEach(([config]) => expect(config.params).not.toHaveProperty('sessionId'))
  })

  it('显式重试身份保留在原请求参数中', () => {
    const sessionId = '0123456789abcdef0123456789abcdef'
    playback({ channelId: 5, startTime: 'start', endTime: 'end', sessionId })
    play(['device', 'channel', 'start', 'end', sessionId])
    expect(request.mock.calls[0][0].params.sessionId).toBe(sessionId)
    expect(request.mock.calls[1][0].params.sessionId).toBe(sessionId)
  })

  it('旧设备入口默认不注入会话身份，保持原共享契约', () => {
    const params = ['device', 'channel', 'start', 'end']
    play(params)
    play(params)
    request.mock.calls.forEach(([config]) => expect(config.params).not.toHaveProperty('sessionId'))
  })

  it('显式非法空身份仍透传给后端校验，不能静默改为另一会话', () => {
    playback({ channelId: 5, startTime: 'start', endTime: 'end', sessionId: '' })
    play(['device', 'channel', 'start', 'end', ''])
    request.mock.calls.forEach(([config]) => expect(config.params.sessionId).toBe(''))
  })
})
