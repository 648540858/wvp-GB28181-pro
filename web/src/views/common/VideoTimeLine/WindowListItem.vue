<template>
  <div ref="windowListItem" class="windowListItem" :class="{active: active}" @click="onClick">
    <span class="order">{{ index + 1 }}</span>
    <canvas ref="canvas" class="windowListItemCanvas" />
  </div>
</template>

<script>
export default {
  name: 'WindowListItem',
  props: {
    index: {
      type: Number
    },
    data: {
      type: Object,
      default() {
        return {}
      }
    },
    totalMS: {
      type: Number
    },
    startTimestamp: {
      type: Number
    },
    width: {
      type: Number
    },
    active: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      height: 0,
      ctx: null
    }
  },
  mounted() {
    this.init()
    this.drawTimeSegments()
  },
  methods: {
    /**
     * @Author: 王林25
     * @Date: 2020-04-14 09:20:22
     * @Desc: 初始化
     */
    init() {
      const { height } = this.$refs.windowListItem.getBoundingClientRect()
      this.height = height - 1
      this.$refs.canvas.width = this.width
      this.$refs.canvas.height = this.height
      this.ctx = this.$refs.canvas.getContext('2d')
    },

    /**
     * @Author: 王林25
     * @Date: 2020-04-14 15:42:49
     * @Desc: 绘制时间段
     */
    drawTimeSegments(callback, path) {
      if (!this.data.timeSegments || this.data.timeSegments.length <= 0) {
        return
      }
      const PX_PER_MS = this.width / this.totalMS // px/ms，每毫秒占的像素
      this.data.timeSegments.forEach((item) => {
        if (
          item.beginTime <= this.startTimestamp + this.totalMS &&
          item.endTime >= this.startTimestamp
        ) {
          this.ctx.beginPath()
          let x = (item.beginTime - this.startTimestamp) * PX_PER_MS
          let w
          if (x < 0) {
            x = 0
            w = (item.endTime - this.startTimestamp) * PX_PER_MS
          } else {
            w = (item.endTime - item.beginTime) * PX_PER_MS
          }
          const heightStartRatio = item.startRatio === undefined ? 0.6 : item.startRatio
          const heightEndRatio = item.endRatio === undefined ? 0.9 : item.endRatio
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

    /**
     * @Author: 王林25
     * @Date: 2020-04-14 14:25:43
     * @Desc: 清除画布
     */
    clearCanvas() {
      this.ctx.clearRect(0, 0, this.width, this.height)
    },

    /**
     * @Author: 王林25
     * @Date: 2021-01-20 19:07:31
     * @Desc: 绘制
     */
    draw() {
      this.$nextTick(() => {
        this.clearCanvas()
        this.drawTimeSegments()
      })
    },

    /**
     * @Author: 王林25
     * @Date: 2021-01-20 19:26:46
     * @Desc: 点击事件
     */
    onClick(e) {
      this.$emit('click', e)
      const { left, top } = this.$refs.windowListItem.getBoundingClientRect()
      const x = e.clientX - left
      const y = e.clientY - top
      const timeSegments = this.getClickTimeSegments(x, y)
      if (timeSegments.length > 0) {
        this.$emit('click_window_timeSegments', timeSegments, this.index, this.data)
      }
    },

    /**
     * @Author: 王林25
     * @Date: 2021-01-20 16:24:54
     * @Desc: 检测当前是否点击了某个时间段
     */
    getClickTimeSegments(x, y) {
      if (!this.data.timeSegments || this.data.timeSegments.length <= 0) {
        return []
      }
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
     * @Date: 2021-01-21 11:25:26
     * @Desc: 获取位置信息
     */
    getRect() {
      return this.$refs.windowListItem ? this.$refs.windowListItem.getBoundingClientRect() : null
    }
  }
}
</script>

<style scoped>
.windowListItem {
  width: 100%;
  height: 30px;
  position: relative;
  border-bottom: 1px solid #999999;
  user-select: none;
}
.windowListItem.active {
  background-color: #000;
}
.windowListItem .order {
  position: absolute;
  width: 30px;
  height: 30px;
  display: flex;
  justify-content: center;
  align-items: center;
  color: #fff;
  border-right: 1px solid #999999;
}

</style>
