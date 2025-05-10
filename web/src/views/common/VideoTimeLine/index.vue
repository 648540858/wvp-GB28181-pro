<template>
  <div
    ref="timeLineContainer"
    class="timeLineContainer"
    :style="{
      backgroundColor: backgroundColor,
    }"
    @touchstart="onTouchstart"
    @touchmove="onTouchmove"
    @mousedown="onMousedown"
    @mouseout="onMouseout"
    @mousemove="onMousemove"
    @mouseleave="onMouseleave"
  >
    <canvas
      ref="canvas"
      class="canvas"
      @mousewheel.stop.prevent="onMouseweel"
    />
    <div
      v-if="showWindowList && windowList && windowList.length > 1"
      ref="windowList"
      class="windowList"
      @scroll="onWindowListScroll"
    >
      <WindowListItem
        v-for="(item, index) in windowListInner"
        ref="WindowListItem"
        :key="index"
        :index="index"
        :data="item"
        :total-m-s="totalMS"
        :start-timestamp="startTimestamp"
        :width="width"
        :active="item.active"
        @click_window_timeSegments="triggerClickWindowTimeSegments"
        @click="toggleActive(index)"
      />
    </div>
  </div>
</template>

<script>
import dayjs from 'dayjs'
import WindowListItem from './WindowListItem'
import {
  ONE_HOUR_STAMP,
  ZOOM,
  ZOOM_HOUR_GRID,
  ZOOM_DATE_SHOW_RULE,
  MOBILE_ZOOM_HOUR_GRID,
  MOBILE_ZOOM_DATE_SHOW_RULE
} from './constant'

/**
 * @Author: 王林25
 * @Date: 2021-01-19 20:15:07
 * @Desc: 时间轴组件
 */
export default {
  name: 'TimeLine',
  components: {
    WindowListItem
  },
  props: {
    // 初始时间，中点所在的时间，默认为当天0点
    initTime: {
      type: [Number, String],
      default: ''
    },
    // 显示预览的时间范围，即中间刻度所允许的时间范围
    /*
      {
        start: '2020-12-19 18:30:00',// 允许显示的最小时间
        end: '2021-01-20 10:0:00'// 允许显示的最大时间
      }
    */
    timeRange: {
      type: Object,
      default() {
        return {}
      }
    },
    // 初始的时间分辨率
    initZoomIndex: {
      type: Number,
      default: 5 // 24小时
    },
    // 是否显示中间的竖线
    showCenterLine: {
      type: Boolean,
      default: true
    },
    // 中间竖线的样式
    centerLineStyle: {
      type: Object,
      default() {
        return {
          width: 2,
          color: '#fff'
        }
      }
    },
    // 日期时间文字颜色
    textColor: {
      type: String,
      default: 'rgba(151,158,167,1)'
    },
    // 鼠标滑过显示的时间文字颜色
    hoverTextColor: {
      type: String,
      default: 'rgb(194, 202, 215)'
    },
    // 时间线段颜色
    lineColor: {
      type: String,
      default: 'rgba(151,158,167,1)'
    },
    // 时间线段高度占时间轴高度的比例
    lineHeightRatio: {
      type: Object,
      default() {
        return {
          date: 0.3, // 0点时的日期线段高度
          time: 0.2, // 显示时间的线段高度
          none: 0.1, // 不显示时间的线段高度
          hover: 0.3 // 鼠标滑过时显示的时间段高度
        }
      }
    },
    // 鼠标滑过时是否显示实时所在的时间
    showHoverTime: {
      type: Boolean,
      default: true
    },
    // 格式化鼠标滑过时间
    hoverTimeFormat: {
      type: Function
    },
    // 要显示的时间颜色段
    /*
      {
        beginTime: new Date('2021-01-19 14:30:00').getTime(),// 起始时间戳
        endTime: new Date('2021-01-20 18:00:00').getTime(),// 结束时间戳
        color: '#FA3239',// 颜色
        startRatio: 0.65,// 高度的起始比例，即top=时间轴高度*startRatio
        endRatio: 0.9// 高度的结束比例，即bottom=时间轴高度*endRatio
      }
    */
    timeSegments: {
      type: Array,
      default: () => []
    },
    // 时间轴背景颜色
    backgroundColor: {
      type: String,
      default: '#262626'
    },
    // 是否允许切换分辨率
    enableZoom: {
      type: Boolean,
      default: true
    },
    // 是否允许拖动
    enableDrag: {
      type: Boolean,
      default: true
    },
    // 窗口列表，如果窗口数量大于1的话可以配置此项，会显示和窗口对应数量的时间轴，只有一个窗口的话请直接使用基本时间轴
    /*
      {
        timeSegments: [// 时间段
          {
            beginTime: new Date('2021-01-19 14:30:00').getTime(),// 起始时间戳
            endTime: new Date('2021-01-20 18:00:00').getTime(),// 结束时间戳
            color: '#FA3239',// 颜色
            startRatio: 0.65,// 高度的起始比例，即top=时间轴高度*startRatio
            endRatio: 0.9// 高度的结束比例，即bottom=时间轴高度*endRatio
          }
        ],
        // 你的其他附加信息...
      }
    */
    windowList: {
      type: Array,
      default() {
        return []
      }
    },
    // 当显示windowList时的基础时间轴高度
    baseTimeLineHeight: {
      type: Number,
      default: 50
    },
    // 初始选中的窗口时间轴
    initSelectWindowTimeLineIndex: {
      type: Number,
      default: -1
    },
    // 是否是手机端
    isMobile: {
      type: Boolean,
      default: false
    },
    // 鼠标按下和松开的距离小于该值认为是点击事件
    maxClickDistance: {
      type: Number,
      default: 3
    },
    // 绘制时间段时对计算出来的坐标进行四舍五入，可以防止相连的时间段绘制出来有间隔的问题
    roundWidthTimeSegments: {
      type: Boolean,
      default: true
    },
    // 自定义显示哪些时间
    customShowTime: {
      type: Function
    },
    // 0点处是否显示日期
    showDateAtZero: {
      type: Boolean,
      default: true
    },
    // 扩展ZOOM列表，这个数组的数据会追加到内部的ZOOM数组，对应的zoomIndex往后累加即可，内部一共有11个zoom，那么你追加了一项，对应的zoomIndex为11，因为是从零开始计数
    // 数组类型，数组的每一项为：
    /*
      {
        zoom: 26,// 时间分辨率，整个时间轴表示的时间范围，单位：小时
        zoomHourGrid: 0.5,// 时间分辨率对应的每格小时数，即时间轴上最小格代表多少小时
        mobileZoomHourGrid: 2, // 手机模式下时间分辨率对应的每格小时数，如果不用适配手机端，可以不用设置
      }
    */
    // 同时你需要传递customShowTime属性来自定义控制时间显示，否则会报错，因为内置的规则只有11个
    extendZOOM: {
      type: Array,
      default() {
        return []
      }
    },
    // 格式化时间轴显示时间
    formatTime: {
      type: Function
    }
  },
  data() {
    return {
      width: 0,
      height: 0,
      ctx: null,
      currentZoomIndex: 0,
      currentTime: 0,
      startTimestamp: 0,
      mousedown: false,
      mousedownX: 0,
      mousedownY: 0,
      mousedownCacheStartTimestamp: 0,
      showWindowList: false,
      windowListInner: [],
      mousemoveX: -1,
      watchTimeList: []
    }
  },
  computed: {
    // 整个时间轴所代表的毫秒数
    totalMS() {
      return ZOOM[this.currentZoomIndex] * ONE_HOUR_STAMP
    },
    // 时间范围的时间戳表示
    timeRangeTimestamp() {
      const t = {}
      if (this.timeRange.start) {
        t.start = typeof this.timeRange.start === 'number' ? this.timeRange.start : new Date(this.timeRange.start).getTime()
      }
      if (this.timeRange.end) {
        t.end = typeof this.timeRange.end === 'number' ? this.timeRange.end : new Date(this.timeRange.end).getTime()
      }
      return t
    },
    ACT_ZOOM_HOUR_GRID() {
      return this.isMobile ? MOBILE_ZOOM_HOUR_GRID : ZOOM_HOUR_GRID
    },
    ACT_ZOOM_DATE_SHOW_RULE() {
      return this.isMobile ? MOBILE_ZOOM_DATE_SHOW_RULE : ZOOM_DATE_SHOW_RULE
    },
    // 年月模式
    yearMonthMode() {
      return this.currentZoomIndex === 9
    },
    // 年模式
    yearMode() {
      return this.currentZoomIndex === 10
    }
  },
  watch: {
    timeSegments: {
      deep: true,
      handler: 'reRender'
    }
  },
  created() {
    this.extendZOOM.forEach((item) => {
      ZOOM.push(item.zoom)
      ZOOM_HOUR_GRID.push(item.zoomHourGrid)
      MOBILE_ZOOM_HOUR_GRID.push(item.mobileZoomHourGrid)
    })
  },
  mounted() {
    this.setInitData()
    this.init()
    this.draw()
    this.onMouseup = this.onMouseup.bind(this)
    this.onResize = this.onResize.bind(this)
    this.onTouchend = this.onTouchend.bind(this)
    if (this.isMobile) {
      window.addEventListener('touchend', this.onTouchend)
    } else {
      window.addEventListener('mouseup', this.onMouseup)
    }
    window.addEventListener('resize', this.onResize)
  },
  beforeDestroy() {
    if (this.isMobile) {
      window.removeEventListener('touchend', this.onTouchend)
    } else {
      window.removeEventListener('mouseup', this.onMouseup)
    }
    window.removeEventListener('resize', this.onResize)
  },
  methods: {
    /**
     * @Author: 王林25
     * @Date: 2021-01-19 20:20:45
     * @Desc: 设置初始数据
     */
    setInitData() {
      // 内部窗口列表数据
      this.windowListInner = this.windowList.map((item, index) => {
        return {
          ...item,
          active: this.initSelectWindowTimeLineIndex === index
        }
      })
      // 必须先设置currentZoomIndex
      // 初始时间分辨率
      this.currentZoomIndex =
        this.initZoomIndex >= 0 && this.initZoomIndex < ZOOM.length
          ? this.initZoomIndex
          : 5
      // 初始当前时间
      this.startTimestamp =
        (this.initTime
          ? typeof this.initTime === 'number' ? this.initTime : new Date(this.initTime).getTime()
          : new Date(dayjs().format('YYYY-MM-DD 00:00:00')).getTime()) -
        this.totalMS / 2
      // 根据时间范围检查并修正起始时间
      this.fixStartTimestamp()
    },

    /**
     * @Author: 王林25
     * @Date: 2021-01-20 16:01:21
     * @Desc: 根据时间范围检查并修正起始时间
     */
    fixStartTimestamp() {
      const hfms = this.totalMS / 2
      const ct = this.startTimestamp + hfms
      if (this.timeRangeTimestamp.start && ct < this.timeRangeTimestamp.start) {
        this.startTimestamp = this.timeRangeTimestamp.start - hfms
      }
      if (this.timeRangeTimestamp.end && ct > this.timeRangeTimestamp.end) {
        this.startTimestamp = this.timeRangeTimestamp.end - hfms
      }
    },

    /**
     * @Author: 王林25
     * @Date: 2020-04-14 09:20:22
     * @Desc: 初始化
     */
    init() {
      const {
        width,
        height
      } = this.$refs.timeLineContainer.getBoundingClientRect()
      this.width = width
      this.height =
        this.windowList.length > 1 ? this.baseTimeLineHeight : height
      this.$refs.canvas.width = this.width
      this.$refs.canvas.height = this.height
      this.ctx = this.$refs.canvas.getContext('2d')
      this.showWindowList = true
    },

    /**
     * @Author: 王林25
     * @Date: 2020-04-14 09:27:18
     * @Desc: 绘制方法
     */
    draw() {
      // 顺序很重要，不然层级不对
      this.drawTimeSegments()
      this.addGraduations()
      this.drawMiddleLine()

      this.currentTime = this.startTimestamp + this.totalMS / 2
      this.$emit('timeChange', this.currentTime)

      // 通知窗口时间轴渲染
      try {
        this.$refs.WindowListItem.forEach((item) => {
          item.draw()
        })
        // eslint-disable-next-line no-empty
      } catch (error) { }

      // 更新观察的时间位置
      this.updateWatchTime()
    },

    /**
     * @Author: 王林25
     * @Date: 2021-01-21 10:50:11
     * @Desc:  更新观察的时间位置
     */
    updateWatchTime() {
      this.watchTimeList.forEach((item) => {
        // 当前不在显示范围内
        if (item.time < this.startTimestamp || item.time > this.startTimestamp + this.totalMS) {
          item.callback(-1, -1)
        } else { // 在范围内
          const x = (item.time - this.startTimestamp) * (this.width / this.totalMS)
          let y = 0
          const { left, top } = this.$refs.canvas.getBoundingClientRect()
          if (item.windowTimeLineIndex !== -1 && this.windowList.length > 1 && item.windowTimeLineIndex >= 0 && item.windowTimeLineIndex < this.windowList.length) {
            const rect = this.$refs.WindowListItem[item.windowTimeLineIndex].getRect()
            y = rect ? rect.top : top
          } else {
            y = top
          }
          item.callback(x + left, y)
        }
      })
    },

    /**
     * @Author: 王林25
     * @Date: 2020-04-14 09:27:46
     * @Desc: 绘制中间的竖线
     */
    drawMiddleLine() {
      if (!this.showCenterLine) {
        return
      }
      this.ctx.beginPath()
      const { width, color } = this.centerLineStyle
      const x = this.width / 2
      this.drawLine(x, 0, x, this.height, width, color)
    },

    /**
     * @Author: 王林25
     * @Date: 2020-04-14 11:03:44
     * @Desc: 绘制时间刻度
     */
    addGraduations() {
      this.ctx.beginPath()
      // 一共可以绘制的格数
      const gridNum =
        ZOOM[this.currentZoomIndex] / this.ACT_ZOOM_HOUR_GRID[this.currentZoomIndex]
      // 一格多少毫秒
      const msPerGrid = this.ACT_ZOOM_HOUR_GRID[this.currentZoomIndex] * ONE_HOUR_STAMP
      // 每格间距，一格多少像素宽
      const pxPerGrid = this.width / gridNum
      // 起始偏移距离
      const msOffset = msPerGrid - (this.startTimestamp % msPerGrid)
      const pxOffset = (msOffset / msPerGrid) * pxPerGrid
      for (let i = 0; i < gridNum; i++) {
        const currentStartTimestamp = this.startTimestamp + msOffset + i * msPerGrid
        let adjustMsOffset = 0
        // 分辨率以年为单位
        if (this.yearMode) {
          adjustMsOffset = currentStartTimestamp - new Date(`${dayjs(currentStartTimestamp).format('YYYY')}-01-01 00:00:00`).getTime()
        } else if (this.yearMonthMode) {
          // 分辨率以月为单位
          adjustMsOffset = currentStartTimestamp - new Date(`${dayjs(currentStartTimestamp).format('YYYY')}-${dayjs(currentStartTimestamp).format('MM')}-01 00:00:00`).getTime()
        }
        const x = pxOffset + i * pxPerGrid - (adjustMsOffset / msPerGrid) * pxPerGrid
        const graduationTime = currentStartTimestamp - adjustMsOffset
        let h = 0
        const date = new Date(graduationTime)
        // 0点显示日期
        if (this.showDateAtZero && date.getHours() === 0 && date.getMinutes() === 0) {
          h = this.height * (this.lineHeightRatio.date === undefined ? 0.3 : this.lineHeightRatio.date)
          this.ctx.fillStyle = this.textColor
          this.ctx.fillText(
            this.graduationTitle(graduationTime),
            x - 13,
            h + 15
          )
        } else if (this.checkShowTime(date)) {
          // 其余时间根据各自规则显示
          h = this.height * (this.lineHeightRatio.time === undefined ? 0.2 : this.lineHeightRatio.time)
          this.ctx.fillStyle = this.textColor
          this.ctx.fillText(
            this.graduationTitle(graduationTime),
            x - 13,
            h + 15
          )
        } else {
          // 不显示时间的线段
          h = this.height * (this.lineHeightRatio.none === undefined ? 0.1 : this.lineHeightRatio.none)
        }
        this.drawLine(x, 0, x, h, 1, this.lineColor)
      }
    },

    // 判断是否需要显示该时间
    checkShowTime(date) {
      if (this.customShowTime) {
        const res = this.customShowTime(date, this.currentZoomIndex)
        if (res === true) {
          return true
        } else if (res === false) {
          return false
        }
      }
      return this.ACT_ZOOM_DATE_SHOW_RULE[this.currentZoomIndex](date)
    },

    /**
     * @Author: 王林25
     * @Date: 2020-04-14 15:42:49
     * @Desc: 绘制时间段
     */
    drawTimeSegments(callback, path) {
      const PX_PER_MS = this.width / this.totalMS // px/ms，每毫秒占的像素
      this.timeSegments.forEach((item) => {
        if (
          item.beginTime <= this.startTimestamp + this.totalMS
        ) {
          const hasEndTime = item.endTime >= this.startTimestamp
          this.ctx.beginPath()
          let x = (item.beginTime - this.startTimestamp) * PX_PER_MS
          let w
          if (x < 0) {
            x = 0
            w = hasEndTime ? (item.endTime - this.startTimestamp) * PX_PER_MS : 1
          } else {
            w = hasEndTime ? (item.endTime - item.beginTime) * PX_PER_MS : 1
          }
          const heightStartRatio = item.startRatio === undefined ? 0.6 : item.startRatio
          const heightEndRatio = item.endRatio === undefined ? 0.9 : item.endRatio
          if (this.roundWidthTimeSegments) {
            x = Math.round(x)
            w = Math.round(w)
          }
          // 避免时间段小于1px绘制不出来
          w = Math.max(1, w)
          if (path) {
            this.ctx.rect(
              x,
              this.height * heightStartRatio,
              w,
              this.height * (heightEndRatio - heightStartRatio)
            )
          } else {
            this.ctx.fillStyle = item.color
            this.ctx.fillRect(
              x,
              this.height * heightStartRatio,
              w,
              this.height * (heightEndRatio - heightStartRatio)
            )
          }
          callback && callback(item)
        }
      })
    },

    // 触摸开始事件
    onTouchstart(e) {
      if (!this.isMobile) {
        return
      }
      e = e.touches[0]
      this.onPointerdown(e)
    },

    /**
     * @Author: 王林25
     * @Date: 2020-04-14 14:29:40
     * @Desc: 鼠标按下事件
     */
    onMousedown(e) {
      if (this.isMobile) {
        return
      }
      this.onPointerdown(e)
    },

    // 按下事件
    onPointerdown(e) {
      const pos = this.getClientOffset(e)
      this.mousedownX = pos[0]
      this.mousedownY = pos[1]
      this.mousedown = true
      this.mousedownCacheStartTimestamp = this.startTimestamp
      this.$emit('mousedown', e)
    },

    // 触摸结束事件
    onTouchend(e) {
      if (!this.isMobile) {
        return
      }
      e = e.touches[0]
      this.onPointerup(e)
    },

    /**
     * @Author: 王林25
     * @Date: 2020-04-14 14:38:30
     * @Desc: 鼠标松开
     */
    onMouseup(e) {
      if (this.isMobile) {
        return
      }
      this.onPointerup(e)
    },

    // 松开事件
    onPointerup(e) {
      // 触发click事件
      const pos = this.getClientOffset(e)
      const reset = () => {
        this.mousedown = false
        this.mousedownX = 0
        this.mousedownY = 0
        this.mousedownCacheStartTimestamp = 0
      }
      if (
        Math.abs(pos[0] - this.mousedownX) <= this.maxClickDistance &&
        Math.abs(pos[1] - this.mousedownY) <= this.maxClickDistance
      ) {
        reset()
        this.onClick(...pos)
        return
      }
      if (this.mousedown && this.enableDrag) {
        reset()
        this.$emit('dragTimeChange', this.currentTime)
      } else {
        reset()
      }
      this.$emit('mouseup', e)
    },

    // 触摸移动事件
    onTouchmove(e) {
      if (!this.isMobile) {
        return
      }
      e = e.touches[0]
      this.onPointermove(e)
    },

    /**
     * @Author: 王林25
     * @Date: 2020-04-14 14:17:02
     * @Desc: 鼠标移动事件
     */
    onMousemove(e) {
      if (this.isMobile) {
        return
      }
      this.onPointermove(e)
    },

    // 移动事件
    onPointermove(e) {
      const x = this.getClientOffset(e)[0]
      this.mousemoveX = x
      // 按下拖动
      if (this.mousedown && this.enableDrag) {
        this.drag(x)
      } else if (this.showHoverTime) {
        // 未按下显示鼠标所在时间
        this.hoverShow(x)
      }
    },

    /**
     * @Author: 王林25
     * @Date: 2021-01-21 10:40:37
     * @Desc: 鼠标移出事件
     */
    onMouseleave() {
      this.mousemoveX = -1
    },

    /**
     * @Author: 王林25
     * @Date: 2021-01-20 15:29:46
     * @Desc: 按下拖动
     */
    drag(x) {
      if (!this.enableDrag) {
        return
      }
      const PX_PER_MS = this.width / this.totalMS // px/ms
      const diffX = x - this.mousedownX
      // 判断是否超出限制范围
      const hfms = this.totalMS / 2
      let _newStartTimestamp =
        this.mousedownCacheStartTimestamp - Math.round(diffX / PX_PER_MS)
      const ct = _newStartTimestamp + hfms
      if (this.timeRangeTimestamp.start && ct < this.timeRangeTimestamp.start) {
        _newStartTimestamp = this.timeRangeTimestamp.start - hfms
      }
      if (this.timeRangeTimestamp.end && ct > this.timeRangeTimestamp.end) {
        _newStartTimestamp = this.timeRangeTimestamp.end - hfms
      }
      this.startTimestamp = _newStartTimestamp
      this.clearCanvas(this.width, this.height)
      this.draw()
    },

    /**
     * @Author: 王林25
     * @Date: 2021-01-20 15:29:52
     * @Desc: 未按下显示鼠标所在时间
     */
    hoverShow(x, noDraw) {
      const PX_PER_MS = this.width / this.totalMS // px/ms
      const time = this.startTimestamp + x / PX_PER_MS
      if (!noDraw) {
        this.clearCanvas(this.width, this.height)
        this.draw()
      }
      const h = this.height * (this.lineHeightRatio.hover === undefined ? 0.3 : this.lineHeightRatio.hover)
      this.drawLine(x, 0, x, h, 1, this.lineColor)
      this.ctx.fillStyle = this.hoverTextColor
      const t = this.hoverTimeFormat ? this.hoverTimeFormat(time) : dayjs(time).format('YYYY-MM-DD HH:mm:ss')
      const w = this.ctx.measureText(t).width
      this.ctx.fillText(t, x - w / 2, h + 20)
    },

    /**
     * @Author: 王林25
     * @Date: 2020-04-14 14:28:48
     * @Desc: 鼠标移出事件
     */
    onMouseout() {
      this.clearCanvas(this.width, this.height)
      this.draw()
    },

    /**
     * @Author: 王林25
     * @Date: 2020-04-14 15:14:12
     * @Desc: 鼠标滚动
     */
    onMouseweel(event) {
      if (!this.enableZoom) {
        return
      }
      const e = window.event || event
      const delta = Math.max(-1, Math.min(1, e.wheelDelta || -e.detail))
      if (delta < 0) {
        if (this.currentZoomIndex + 1 >= ZOOM.length - 1) {
          this.currentZoomIndex = ZOOM.length - 1
        } else {
          this.currentZoomIndex++
        }
      } else if (delta > 0) {
        // 放大
        if (this.currentZoomIndex - 1 <= 0) {
          this.currentZoomIndex = 0
        } else {
          this.currentZoomIndex--
        }
      }
      this.clearCanvas(this.width, this.height)
      this.startTimestamp = this.currentTime - this.totalMS / 2 // 当前时间-新的时间范围的一半
      this.draw()
    },

    /**
     * @Author: 王林25
     * @Date: 2021-01-20 16:22:04
     * @Desc: 点击事件
     */
    onClick(x, y) {
      const PX_PER_MS = this.width / this.totalMS // px/ms
      const time = this.startTimestamp + x / PX_PER_MS
      const date = dayjs(time).format('YYYY-MM-DD HH:mm:ss')
      const timeSegments = this.getClickTimeSegments(x, y)
      if (timeSegments && timeSegments.length > 0) {
        this.$emit('click_timeSegments', timeSegments, time, date, x)
      } else {
        this.onCanvasClick(time, date, x)
      }
    },

    /**
     * @Author: 王林25
     * @Date: 2021-01-20 16:24:54
     * @Desc: 检测当前是否点击了某个时间段
     */
    getClickTimeSegments(x, y) {
      const inItems = []
      this.drawTimeSegments((item) => {
        if (this.ctx.isPointInPath(x, y)) {
          inItems.push(item)
        }
      }, true)
      return inItems
    },

    /**
     * @Author: 王林25
     * @Date: 2021-01-20 11:14:30
     * @Desc: 获取鼠标相当于时间轴的距离
     */
    getClientOffset(e) {
      if (!this.$refs.timeLineContainer || !e) {
        return [0, 0]
      }
      const { left, top } = this.$refs.timeLineContainer.getBoundingClientRect()
      return [e.clientX - left, e.clientY - top]
    },

    /**
     * @Author: 王林25
     * @Date: 2020-04-14 14:25:43
     * @Desc: 清除画布
     */
    clearCanvas(w, h) {
      this.ctx.clearRect(0, 0, w, h)
    },

    /**
     * @Author: 王林25
     * @Date: 2020-04-14 14:15:25
     * @Desc: 时间格式化
     */
    graduationTitle(datetime) {
      const time = dayjs(datetime)
      let res = ''
      if (this.formatTime) {
        res = this.formatTime(time)
      }
      if (res) {
        return res
      }
      if (this.yearMode) {
        return time.format('YYYY')
      } else if (this.yearMonthMode) {
        return time.format('YYYY-MM')
      } else if (
        time.hour() === 0 &&
        time.minute() === 0 &&
        time.millisecond() === 0
      ) {
        return time.format('MM-DD')
      } else {
        return time.format('HH:mm')
      }
    },

    /**
     * @Author: 王林25
     * @Date: 2020-04-14 11:28:37
     * @Desc: 绘制线段
     */
    drawLine(x1, y1, x2, y2, lineWidth = 1, color = '#fff') {
      this.ctx.beginPath()
      this.ctx.strokeStyle = color
      this.ctx.lineWidth = lineWidth
      this.ctx.moveTo(x1, y1)
      this.ctx.lineTo(x2, y2)
      this.ctx.stroke()
    },

    /**
     * @Author: 王林25
     * @Date: 2021-01-20 15:57:11
     * @Desc: 重新渲染
     */
    reRender() {
      this.$nextTick(() => {
        this.clearCanvas(this.width, this.height)
        this.reset()
        this.setInitData()
        this.init()
        this.draw()
      })
    },

    /**
     * @Author: 王林25
     * @Date: 2021-01-20 16:07:53
     * @Desc: 复位
     */
    reset() {
      this.width = 0
      this.height = 0
      this.ctx = null
      this.currentZoomIndex = 0
      this.currentTime = 0
      this.startTimestamp = 0
      this.mousedown = false
      this.mousedownX = 0
      this.mousedownCacheStartTimestamp = 0
    },

    /**
     * @Author: 王林25
     * @Date: 2021-01-20 15:57:26
     * @Desc: 设置当前时间
     */
    setTime(t) {
      if (this.mousedown) {
        return
      }
      const ts = typeof t === 'number' ? t : new Date(t).getTime()
      this.startTimestamp = ts - this.totalMS / 2
      this.fixStartTimestamp()
      this.clearCanvas(this.width, this.height)
      this.draw()
      if (this.mousemoveX !== -1 && !this.isMobile) {
        this.hoverShow(this.mousemoveX, true)
      }
    },

    /**
     * @Author: 王林25
     * @Date: 2021-01-20 19:32:39
     * @Desc: 转发窗口时间轴的事件
     */
    triggerClickWindowTimeSegments(data, index, item) {
      this.$emit('click_window_timeSegments', data, index, item)
    },

    /**
     * @Author: 王林25
     * @Date: 2021-01-21 09:58:17
     * @Desc: 设置分辨率
     */
    setZoom(index) {
      this.currentZoomIndex =
        index >= 0 && index < ZOOM.length
          ? index
          : 5
      this.clearCanvas(this.width, this.height)
      this.startTimestamp = this.currentTime - this.totalMS / 2 // 当前时间-新的时间范围的一半
      this.draw()
    },

    /**
     * @Author: 王林25
     * @Date: 2021-01-21 10:15:30
     * @Desc: 切换窗口时间轴的选中
     */
    toggleActive(index) {
      this.windowListInner.forEach((item) => {
        item.active = false
      })
      this.windowListInner[index].active = true
      this.$emit('change_window_time_line', index, this.windowListInner[index])
    },

    /**
     * @Author: 王林25
     * @Date: 2021-01-21 10:47:28
     * @Desc: 要观察的时间点，会返回该时间点的实时位置，你可以根据该位置来设置一些你的自定义元素，位置为相对于浏览器可视窗口的位置
     */
    watchTime(time, callback, windowTimeLineIndex) {
      if (!time || !callback) {
        return
      }
      this.watchTimeList.push({
        time: typeof time === 'number' ? time : new Date(time).getTime(),
        callback,
        windowTimeLineIndex: typeof windowTimeLineIndex === 'number' ? windowTimeLineIndex - 1 : -1
      })
    },

    /**
     * @Author: 王林25
     * @Date: 2021-01-21 13:36:37
     * @Desc: 窗口时间轴滚动
     */
    onWindowListScroll() {
      this.updateWatchTime()
    },

    /**
     * @Author: 王林25
     * @Date: 2021-01-21 13:40:53
     * @Desc: 尺寸重适应
     */
    onResize() {
      this.init()
      this.draw()
      try {
        this.$refs.WindowListItem.forEach((item) => {
          item.init()
        })
        // eslint-disable-next-line no-empty
      } catch (error) { }
    },

    // 时间轴点击事件
    onCanvasClick(...args) {
      this.$emit('click_timeline', ...args)
    }
  }
}
</script>

<style>
.timeLineContainer {
  width: 100%;
  height: 100%;
  cursor: pointer;
  display: flex;
  flex-direction: column;
}
.timeLineContainer .canvas {
  flex-grow: 0;
  flex-shrink: 0;
}
.timeLineContainer .windowList {
  width: 100%;
  height: 100%;
  overflow: auto;
  overflow-x: hidden;
  border-top: 1px solid #999999;
  display: flex;
  flex-direction: column;
}
.timeLineContainer .windowList::-webkit-scrollbar {
  display: none;
}

</style>
