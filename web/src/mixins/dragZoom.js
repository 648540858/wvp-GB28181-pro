export default {
  data() {
    return {
      dragGridEnabled: true,
      overlayCanvas: null,
      overlayCtx: null,
      dragActive: false,
      dragStart: null,
      dragCurrent: null,
      dragVideoRect: null,
      dragCallback: null
    }
  },
  computed: {
    dragRect() {
      if (!this.dragStart || !this.dragCurrent) return null
      return {
        left: Math.min(this.dragStart.x, this.dragCurrent.x),
        top: Math.min(this.dragStart.y, this.dragCurrent.y),
        width: Math.abs(this.dragCurrent.x - this.dragStart.x),
        height: Math.abs(this.dragCurrent.y - this.dragStart.y)
      }
    },
    dragInfo() {
      if (!this.dragRect) return null
      return {
        midX: Math.round(this.dragRect.left + this.dragRect.width / 2),
        midY: Math.round(this.dragRect.top + this.dragRect.height / 2),
        width: Math.round(this.dragRect.width),
        height: Math.round(this.dragRect.height)
      }
    }
  },
  beforeDestroy() {
    this._removeCanvas()
  },
  methods: {
    getVideoElement() {
      return null
    },
    _ensureCanvas() {
      this._removeCanvas()
      const videoRect = this.getVideoRect()
      if (!videoRect) return null
      const parentRect = this.$el.getBoundingClientRect()
      const w = Math.round(videoRect.width)
      const h = Math.round(videoRect.height)
      const canvas = document.createElement('canvas')
      canvas.style.position = 'absolute'
      canvas.style.left = (videoRect.left - parentRect.left) + 'px'
      canvas.style.top = (videoRect.top - parentRect.top) + 'px'
      canvas.style.width = w + 'px'
      canvas.style.height = h + 'px'
      canvas.width = w
      canvas.height = h
      canvas.style.zIndex = '999'
      canvas.style.pointerEvents = 'none'
      console.log('this.dragGridEnabled： ' + this.dragGridEnabled)
      if (this.dragGridEnabled) {
        console.log('加载网格背景')
        canvas.style.backgroundImage =
          'linear-gradient(rgba(64, 158, 255, 0.3) 1px, transparent 2px),' +
          'linear-gradient(90deg, rgba(64, 158, 255, 0.3) 1px, transparent 2px)'
        canvas.style.backgroundSize = '25px 25px'
        canvas.style.border = '2px solid #409EFF'
      }

      this.$el.appendChild(canvas)
      console.log(this.$el)
      const ctx = canvas.getContext('2d')
      this.overlayCanvas = canvas
      this.overlayCtx = ctx
      return { canvas, ctx }
    },
    _removeCanvas() {
      this._unbindDragEvents()
      if (this.overlayCanvas && this.overlayCanvas.parentNode) {
        this.overlayCanvas.parentNode.removeChild(this.overlayCanvas)
      }
      this.overlayCanvas = null
      this.overlayCtx = null
    },
    _bindDragEvents() {
      const c = this.overlayCanvas
      if (!c) return
      c.style.pointerEvents = 'auto'
      c.style.cursor = 'crosshair'
      c.addEventListener('mousedown', this._onDragMouseDown)
      c.addEventListener('mousemove', this._onDragMove)
      c.addEventListener('mouseup', this._onDragEnd)
      c.addEventListener('mouseleave', this._onDragEnd)
    },
    _unbindDragEvents() {
      const c = this.overlayCanvas
      if (!c) return
      c.style.pointerEvents = 'none'
      c.style.cursor = 'default'
      c.removeEventListener('mousedown', this._onDragMouseDown)
      c.removeEventListener('mousemove', this._onDragMove)
      c.removeEventListener('mouseup', this._onDragEnd)
      c.removeEventListener('mouseleave', this._onDragEnd)
    },
    _drawOverlay() {
      const ctx = this.overlayCtx
      const canvas = this.overlayCanvas
      if (!ctx || !canvas) return
      ctx.clearRect(0, 0, canvas.width, canvas.height)
      if (this.dragRect) {
        this._drawDragRect(ctx)
      }
    },
    _drawDragRect(ctx) {
      const r = this.dragRect
      if (!r) return
      ctx.strokeStyle = '#409EFF'
      ctx.lineWidth = 2
      ctx.setLineDash([6, 3])
      ctx.fillStyle = 'rgba(64, 158, 255, 0.15)'
      ctx.beginPath()
      ctx.rect(r.left, r.top, r.width, r.height)
      ctx.fill()
      ctx.stroke()
      ctx.setLineDash([])
      const info = this.dragInfo
      if (!info) return
      const text = '\u4E2D\u5FC3: (' + info.midX + ', ' + info.midY + ') \u5927\u5C0F: ' + info.width + ' \u00D7 ' + info.height
      ctx.font = '12px sans-serif'
      const textW = ctx.measureText(text).width
      const labelW = textW + 16
      const labelH = 22
      const labelX = r.left
      const labelY = r.top + r.height + 6
      ctx.fillStyle = 'rgba(0, 0, 0, 0.7)'
      ctx.fillRect(labelX, labelY, labelW, labelH)
      ctx.fillStyle = '#fff'
      ctx.fillText(text, labelX + 8, labelY + 15)
    },
    startDragZoom(callback) {
      this._ensureCanvas()
      this._bindDragEvents()
      this.dragCallback = callback || null
      this.dragActive = true
      this.dragStart = null
      this.dragCurrent = null
      this.dragVideoRect = null
    },
    _onDragMouseDown(e) {
      if (!this.dragActive) return
      e.preventDefault()
      const videoRect = this.getVideoRect()
      if (!videoRect) return
      this.dragVideoRect = videoRect
      this.dragStart = {
        x: e.clientX - videoRect.left,
        y: e.clientY - videoRect.top
      }
      this.dragCurrent = { ...this.dragStart }
      console.log('[dragZoom mousedown] getVideoRect:', JSON.stringify(videoRect), 'clientX/Y:', e.clientX, e.clientY, 'dragStart:', JSON.stringify(this.dragStart))
      this._drawOverlay()
    },
    _onDragMove(e) {
      if (!this.dragActive || !this.dragStart || !this.dragVideoRect) return
      e.preventDefault()
      this.dragCurrent = {
        x: e.clientX - this.dragVideoRect.left,
        y: e.clientY - this.dragVideoRect.top
      }
      this._drawOverlay()
    },
    _onDragEnd() {
      if (!this.dragActive) return
      if (!this.dragStart || !this.dragCurrent) return
      const sx = Math.min(this.dragStart.x, this.dragCurrent.x)
      const sy = Math.min(this.dragStart.y, this.dragCurrent.y)
      const ex = Math.max(this.dragStart.x, this.dragCurrent.x)
      const ey = Math.max(this.dragStart.y, this.dragCurrent.y)
      const rectW = ex - sx
      const rectH = ey - sy
      if (rectW < 10 || rectH < 10) {
        this._resetDrag()
        return
      }
      console.log('[dragZoom dragEnd] sx:', sx, 'sy:', sy, 'ex:', ex, 'ey:', ey, 'rectW:', rectW, 'rectH:', rectH)
      if (this.dragCallback) {
        const params = {
          length: Math.round(this.dragVideoRect.width),
          width: Math.round(this.dragVideoRect.height),
          midPointX: Math.round(sx + rectW / 2),
          midPointY: Math.round(sy + rectH / 2),
          lengthX: Math.round(rectW),
          lengthY: Math.round(rectH)
        }
        console.log('[dragZoom dragEnd] callback params:', JSON.stringify(params))
        this.dragCallback(params)
      }
      this._resetDrag()
    },
    _resetDrag() {
      this._unbindDragEvents()
      this.dragActive = false
      this.dragStart = null
      this.dragCurrent = null
      this.dragVideoRect = null
      this.dragCallback = null
      this._removeCanvas()
    }
  }
}
