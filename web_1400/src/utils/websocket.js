const socket = {
  ws: null,
  url: 'ws://localhost:8612/websocket',
  // 'blob' or 'arraybuffer'
  binaryType: 'blob',
  ping: 'heartbeat',
  timeout: 15 * 1000,
  // 心跳时间 单位：毫秒
  pingTimeout: 10 * 1000,
  // 重连间隔时间 单位：毫秒
  reconnInterval: 2000,
  timeoutObj: null,
  pingTimeoutObj: null,
  // 重连标识
  lockReconnect: false,
  // 重连限制
  repeatLimit: 5,
  // 重连次数
  repeat: 0,
  // 手动关闭连接，不再重连
  closed: false,
  // 初始化
  init() {
    this.ws = new WebSocket(this.url)
    this.ws.binaryType = this.binaryType
    this.ws.onopen = function(event) {
      console.log('onopen:' + JSON.stringify(event))
      socket.heartCheckStart()
    }
    this.ws.onmessage = function(event) {
      console.log('onmessage:' + JSON.stringify(event.data))
      socket.heartCheckReset()
      socket.heartCheckStart()
    }
    this.ws.onclose = function(event) {
      console.log('onclose:' + JSON.stringify(event))
      socket.heartCheckReconn()
    }
    this.ws.onerror = function(event) {
      console.log('onerror:' + JSON.stringify(event))
      socket.heartCheckReconn()
    }
  },
  // 发送消息
  send(data) {
    console.log('send:' + JSON.stringify(data))
    this.ws.send(JSON.stringify(data))
  },
  // 手动关闭
  close() {
    this.closed = true
    socket.heartCheckReset()
    this.ws.close()
  },
  // 心跳检测重置
  heartCheckReset() {
    const that = this
    clearTimeout(that.timeoutObj)
    clearTimeout(that.pingTimeoutObj)
  },
  // 心跳检测开始
  heartCheckStart() {
    const that = this
    // 不再重连就不再执行心跳
    if (that.closed) return
    that.timeoutObj = setTimeout(function() {
      // 发送一个心跳，后端收到后，返回一个心跳消息，onmessage拿到返回的心跳就说明连接正常
      that.send({ 'command': that.ping })
      // 如果超过一定时间还没重置，说明后端主动断开了
      that.pingTimeoutObj = setTimeout(function() {
        that.ws.close()
      }, that.pingTimeout)
    }, that.timeout)
  },
  // 心跳检测重连
  heartCheckReconn() {
    const that = this
    if (that.repeatLimit > 0 && that.repeatLimit <= that.repeat) return
    if (that.lockReconnect || that.closed) return
    that.lockReconnect = true
    that.repeat++
    // 没连接上会一直重连，设置延迟避免请求过多
    setTimeout(() => {
      that.init()
      that.lockReconnect = false
    }, that.reconnInterval)
  }
}

export default socket

