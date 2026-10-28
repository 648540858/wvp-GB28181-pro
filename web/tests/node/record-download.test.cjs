const assert = require('node:assert/strict')
const { readFileSync } = require('node:fs')
const { resolve } = require('node:path')
const { test } = require('node:test')
const { runInNewContext } = require('node:vm')

// 直接运行组件脚本验证状态及请求归属，避免为最窄回归引入新的测试依赖。
function loadComponent() {
  const source = readFileSync(resolve(__dirname, '../../src/views/dialog/recordDownload.vue'), 'utf8')
  const script = source.match(/<script>([\s\S]*?)<\/script>/)[1]
    .replace(/import elDragDialog from '[^']+'/, 'const elDragDialog = {}')
    .replace('export default', 'module.exports =')
  const context = {
    module: { exports: {} },
    window: { clearTimeout, addEventListener() {}, removeEventListener() {} },
    location: { protocol: 'https:' },
    setTimeout,
    clearTimeout
  }
  runInNewContext(script, context, { filename: 'recordDownload.vue' })
  return context.module.exports
}

// 保留下载副作用计数，并让每次请求的返回值可由测试按真实顺序控制。
function fixture(dispatch = () => Promise.resolve()) {
  const component = loadComponent()
  const calls = { downloads: 0, stops: 0, warnings: 0, schedules: 0 }
  const vm = Object.assign(component.data(), {
    $store: { dispatch },
    $axios: () => { calls.stops++; return Promise.resolve({}) },
    $message: { warning: () => { calls.warnings++ } }
  })
  for (const [name, method] of Object.entries(component.methods)) vm[name] = method.bind(vm)
  vm.downloadFileClientEvent = () => { calls.downloads++ }
  vm.getProgressTimer = () => { calls.schedules++ }
  vm.openDialog('device', 'channel', 'rtp', 'stream', 'media')
  return { vm, calls, component }
}

// 为竞争条件提供可延迟完成的读取响应，不重试任何开始或停止命令。
function deferred() {
  let resolvePromise
  const promise = new Promise(resolve => { resolvePromise = resolve })
  return { promise, resolve: resolvePromise }
}

test('短文件中断时保留实际进度和手动入口，不自动下载也不报100%', async() => {
  const { vm, calls } = fixture(() => Promise.resolve({
    progress: 1, downloadStatus: 'interrupted', downloadedDuration: 115.263,
    downloadMessage: 'private internal detail',
    partialDownloadFilePath: { httpsPath: 'https://example.invalid/partial.mp4' }
  }))
  await vm.getProgress(vm.getProgressTimer)
  assert.equal(vm.downloadStatus, 'interrupted')
  assert.ok(vm.percentage < 100)
  assert.match(vm.title, /中断/)
  assert.match(vm.statusDescription, /1分55秒/)
  assert.doesNotMatch(vm.statusDescription, /private internal detail/)
  assert.equal(vm.downloadFile, 'https://example.invalid/partial.mp4')
  assert.equal(calls.downloads, 0)
  assert.equal(vm.getProgressRun, false)
})

test('新服务仅在完成且文件就绪后显示100%并自动下载一次', async() => {
  const { vm, calls } = fixture(() => Promise.resolve({
    progress: 1, downloadStatus: 'completed', downloadedDuration: 600,
    downLoadFilePath: { httpsPath: 'https://example.invalid/full.mp4' }
  }))
  await vm.getProgress(vm.getProgressTimer)
  await vm.getProgress(vm.getProgressTimer)
  assert.equal(vm.percentage, 100)
  assert.equal(calls.downloads, 1)
  assert.equal(vm.getProgressRun, false)
})

test('已有文件但仍处于封装状态时不能当成完整下载', async() => {
  const { vm, calls } = fixture(() => Promise.resolve({
    progress: 1, downloadStatus: 'finalizing',
    downLoadFilePath: { httpsPath: 'https://example.invalid/slice.mp4' }
  }))
  await vm.getProgress(vm.getProgressTimer)
  assert.ok(vm.percentage < 100)
  assert.equal(vm.downloadFile, null)
  assert.equal(calls.downloads, 0)
  assert.equal(vm.getProgressRun, true)
})

test('旧服务未返回状态时兼容原完整文件入口', async() => {
  const { vm, calls } = fixture(() => Promise.resolve({
    progress: 1, downLoadFilePath: { httpsPath: 'https://example.invalid/legacy.mp4' }
  }))
  await vm.getProgress(vm.getProgressTimer)
  assert.equal(vm.percentage, 100)
  assert.equal(calls.downloads, 1)
})

test('旧请求迟到时不能把重开的新下载弹窗设置为完成', async() => {
  const old = deferred()
  const { vm, calls } = fixture(() => old.promise)
  const reading = vm.getProgress(vm.getProgressTimer)
  vm.close()
  vm.openDialog('device', 'channel', 'rtp', 'new-stream', 'media')
  old.resolve({ progress: 1, downloadStatus: 'completed', downLoadFilePath: { httpsPath: 'https://example.invalid/old.mp4' } })
  await reading
  assert.equal(vm.stream, 'new-stream')
  assert.equal(vm.downloadFile, null)
  assert.equal(vm.percentage, 0)
  assert.equal(calls.downloads, 0)
})

test('首次进度请求前通过对话框关闭会释放自己的会话，重复关闭不重复停止', () => {
  const { vm, calls } = fixture()
  vm.showDialog = false
  vm.close()
  vm.close()
  assert.equal(calls.stops, 1)
  assert.equal(vm.getProgressRun, false)
})

test('已中断的部分结果关闭后保留，不再次调用停止清除结果', async() => {
  const { vm, calls } = fixture(() => Promise.resolve({ progress: 0.19, downloadStatus: 'interrupted', downloadedDuration: 115 }))
  await vm.getProgress(vm.getProgressTimer)
  vm.close()
  assert.equal(calls.stops, 0)
})

test('一次读取失败仍可继续轮询并恢复，连续三次失败才提示停止', async() => {
  const results = [undefined, { progress: 0.2, downloadStatus: 'downloading' }, undefined, undefined, undefined]
  const { vm, calls } = fixture(() => Promise.resolve(results.shift()))
  await vm.getProgress(vm.getProgressTimer)
  assert.equal(vm.getProgressRun, true)
  await vm.getProgress(vm.getProgressTimer)
  assert.equal(vm.progressFailCount, 0)
  assert.equal(vm.percentage, 20)
  await vm.getProgress(vm.getProgressTimer)
  await vm.getProgress(vm.getProgressTimer)
  await vm.getProgress(vm.getProgressTimer)
  assert.equal(vm.getProgressRun, false)
  assert.equal(calls.warnings, 1)
})


test('中断结果只使用独立部分地址，不把旧完整字段当部分文件', async() => {
  const { vm, calls } = fixture(() => Promise.resolve({
    progress: 0.19, downloadStatus: 'interrupted', downloadedDuration: 115,
    downLoadFilePath: { httpsPath: 'https://example.invalid/old.mp4' },
    partialDownloadFilePath: { httpsPath: 'https://example.invalid/partial.mp4' }
  }))
  await vm.getProgress(vm.getProgressTimer)
  assert.equal(vm.downloadFile, 'https://example.invalid/partial.mp4')
  assert.equal(calls.downloads, 0)
})

test('仅部分地址且未声明完成不能触发旧服务兼容成功', async() => {
  const { vm, calls } = fixture(() => Promise.resolve({
    progress: 0.19, partialDownloadFilePath: { httpsPath: 'https://example.invalid/partial.mp4' }
  }))
  await vm.getProgress(vm.getProgressTimer)
  assert.equal(vm.downloadFile, null)
  assert.equal(vm.percentage, 19)
  assert.equal(calls.downloads, 0)
  assert.equal(vm.getProgressRun, true)
})

test('完整终态始终使用原完整字段，部分字段不替换最终成品', async() => {
  const { vm, calls } = fixture(() => Promise.resolve({
    progress: 1, downloadStatus: 'completed',
    downLoadFilePath: { httpsPath: 'https://example.invalid/full.mp4' },
    partialDownloadFilePath: { httpsPath: 'https://example.invalid/partial.mp4' }
  }))
  await vm.getProgress(vm.getProgressTimer)
  assert.equal(vm.downloadFile, 'https://example.invalid/full.mp4')
  assert.equal(vm.percentage, 100)
  assert.equal(calls.downloads, 1)
})
