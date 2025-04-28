<template>
  <div id="weekTimePicker" class="week-time-picker">
    <el-row style="margin-left: 0">
      <el-col>
        <div>
          <el-row style="margin-left: 0">
            <el-col :span="24">
              <div class="time-select-header">
                <el-button @click="selectAll()">全选</el-button>
                <el-button @click="clearTrack()">清空</el-button>
                <el-button @click="removeSelectedTrack()">删除</el-button>
              </div>
              <el-row>
                <el-col :span="20" :offset="2">
                  <div class="time-plan-ruler" style="width: 100%">
                    <div v-for="index in 24*4" :key="index" :class="rulerClass(index - 1)" style="width: 1.04167%;">
                      <span v-if="index === 0 || (index - 1) % 4 === 0 " class="ruler-text">{{
                        (index - 1) / 4
                      }}</span>
                      <span v-if="index === 24*4" class="ruler-text">24</span>
                    </div>
                  </div>
                </el-col>
              </el-row>
              <el-row v-for="(week, index) in weekData" :key="index" class="time-select-main-container">
                <el-col :span="2" class="label">{{ week.name }}</el-col>
                <el-col :span="20">
                  <div class="day-plan" @mousedown="dayPlanMousedown($event, index)">
                    <div v-for="(track, trackIndex) in week.data" :key="trackIndex" class="track" :style="getTrackStyle(track)" @click.stop="selectTrack(trackIndex, index)" @mousedown.stop="">
                      <el-tooltip v-show="checkSelected(trackIndex, index)" :ref="'startPointToolTip-' + index + '-' + trackIndex" :content="getTooltip(track.start)" :placement="(track.end - track.start) < 100 ? 'bottom': 'top'" :manual="true" :value="checkSelected(trackIndex, index)" effect="light" transition="el-zoom-in-top">
                        <div ref="startPoint" class="hand" style="left: 0%" @mousedown.stop="startPointMousedown($event, index, trackIndex)" />
                      </el-tooltip>
                      <el-tooltip v-show="checkSelected(trackIndex, index)" :ref="'endPointToolTip-' + index + '-' + trackIndex" :content="getTooltip(track.end)" placement="top" :manual="true" :value="checkSelected(trackIndex, index)" effect="light" transition="el-zoom-in-top">
                        <div class="hand" style="left: 100%;" @mousedown.stop="endPointMousedown($event, index, trackIndex)" />
                      </el-tooltip>
                    </div>
                    <div v-if="tempTrack.index === index" class="track" :style="getTrackStyle(tempTrack)" />
                  </div>
                </el-col>
                <el-col :span="2" class="operate">
                  <el-popover
                    :ref="'copyBox' + index"
                    placement="right"
                    width="400"
                    trigger="click"
                  >
                    <div>
                      <el-form size="mini" :inline="true">
                        <el-form-item v-for="(data, indexForCopy) in weekDataForCopy(index)" :key="indexForCopy" :label="data.weekData.name">
                          <el-checkbox v-model="weekDataCheckBox[data.index]" />
                        </el-form-item>
                        <el-form-item>
                          <div style="float: right;">
                            <el-button @click="weekDataCheckBoxForAll(index)">全选</el-button>
                            <el-button type="primary" @click="onSubmitCopy(index)">确认</el-button>
                            <el-button @click="closeCopyBox(index)">取消</el-button>
                          </div>
                        </el-form-item>
                      </el-form>
                    </div>
                    <el-button slot="reference" type="text" size="medium">复制</el-button>
                  </el-popover>
                </el-col>
              </el-row>
            </el-col>
          </el-row>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script>

export default {
  name: 'WeekTimePicker',
  props: ['planArray'],
  emits: ['update:planArray'],
  data() {
    return {
      weekData: [
        {
          name: '星期一',
          data: []
        },
        {
          name: '星期二',
          data: []
        },
        {
          name: '星期三',
          data: []
        },
        {
          name: '星期四',
          data: []
        },
        {
          name: '星期五',
          data: []
        },
        {
          name: '星期六',
          data: []
        },
        {
          name: '星期天',
          data: []
        }

      ],
      weekDataCheckBox: [false, false, false, false, false, false, false],
      selectedTrack: {
        trackIndex: null,
        index: null
      },
      tempTrack: {
        index: null,
        start: null,
        end: null,
        x: null,
        clientWidth: null
      },
      startPointTrack: {
        index: null,
        trackIndex: null,
        x: null,
        clientWidth: null,
        target: null
      },
      endPointTrack: {
        index: null,
        trackIndex: null,
        x: null,
        clientWidth: null,
        target: null
      }
    }
  },
  computed: {
    value: {
      get() {
        return this.modelValue
      },
      set(value) {
        this.$emit('update:modelValue', value)
      }
    }
  },
  watch: {
    planArray: function(array) {
      for (let i = 0; i < array.length; i++) {
        this.weekData[i].data = array[i].data
      }
    }
  },
  created() {
    document.addEventListener('click', () => {
      this.selectedTrack.trackIndex = null
      this.selectedTrack.index = null
    })
    document.addEventListener('mousemove', this.dayPlanMousemove)
    document.addEventListener('mouseup', this.dayPlanMouseup)
  },
  methods: {
    rulerClass(index) {
      if (index === 0 || index % 4 === 0) {
        return 'hour ruler-section'
      } else {
        return 'ruler-section'
      }
    },
    checkSelected(trackIndex, index) {
      return index === this.selectedTrack.index && trackIndex === this.selectedTrack.trackIndex
    },
    selectTrack(trackIndex, index) {
      console.log(index)
      if (this.selectedTrack === index * 1000 + trackIndex) {
        return
      }
      this.selectedTrack.index = index
      this.selectedTrack.trackIndex = trackIndex
    },
    getTrackStyle(track) {
      const width = (100 / 24 / 60) * (track.end - track.start)
      const left = (100 / 24 / 60) * track.start
      return `left: ${left}%; width: ${width}%;`
    },
    getTooltip(time) {
      const hour = Math.floor(time / 60)
      const hourStr = hour < 10 ? '0' + hour : hour
      const minuteStr = (time - hour * 60) < 10 ? '0' + Math.floor((time - hour * 60)) : Math.floor(time - hour * 60)
      return hourStr + ':' + minuteStr
    },
    dayPlanMousedown(event, index) {
      this.tempTrack.index = index
      this.tempTrack.start = event.offsetX / event.target.clientWidth * 24 * 60
      this.tempTrack.x = event.screenX
      this.tempTrack.clientWidth = event.target.clientWidth
      this.selectedTrack.index = null
      this.selectedTrack.trackIndex = null
    },
    startPointMousedown(event, index, trackIndex) {
      this.startPointTrack.index = index
      this.startPointTrack.trackIndex = trackIndex
      this.startPointTrack.x = event.screenX
      this.startPointTrack.clientWidth = event.target.parentNode.parentNode.clientWidth
      this.startPointTrack.target = event.target
    },
    endPointMousedown(event, index, trackIndex) {
      this.endPointTrack.index = index
      this.endPointTrack.trackIndex = trackIndex
      this.endPointTrack.x = event.screenX
      this.endPointTrack.clientWidth = event.target.parentNode.parentNode.clientWidth
      this.endPointTrack.target = event.target
    },
    dayPlanMousemove(event) {
      if (this.tempTrack.index !== null) {
        if (event.screenX - this.tempTrack.x === 0) {
          return
        }
        let end = (event.screenX - this.tempTrack.x) / this.tempTrack.clientWidth * 24 * 60 + this.tempTrack.start
        if (end > 24 * 60) {
          end = 24 * 60
        }
        this.tempTrack.end = end
      } else if (this.startPointTrack.trackIndex !== null) {
        if (event.screenX - this.startPointTrack.x === 0) {
          return
        }
        let start = (event.screenX - this.startPointTrack.x) / this.startPointTrack.clientWidth * 24 * 60 +
          this.weekData[this.startPointTrack.index].data[this.startPointTrack.trackIndex].start

        if (start < 0) {
          start = 0
        }
        this.weekData[this.startPointTrack.index].data[this.startPointTrack.trackIndex].start = start
        this.startPointTrack.x = event.screenX
        // 设置提示框位置
        this.$refs[`startPointToolTip-${this.startPointTrack.index}-${this.startPointTrack.trackIndex}`][0].popperElm.style.left = this.startPointTrack.target.getBoundingClientRect().left - 20 + 'px'
        this.updateValue()
      } else if (this.endPointTrack.trackIndex !== null) {
        if (event.screenX - this.endPointTrack.x === 0) {
          return
        }
        let end = (event.screenX - this.endPointTrack.x) / this.endPointTrack.clientWidth * 24 * 60 +
          this.weekData[this.endPointTrack.index].data[this.endPointTrack.trackIndex].end
        if (end > 24 * 60) {
          end = 24 * 60
        }
        this.weekData[this.endPointTrack.index].data[this.endPointTrack.trackIndex].end = end
        this.endPointTrack.x = event.screenX
        // 设置提示框位置
        this.$refs[`endPointToolTip-${this.endPointTrack.index}-${this.endPointTrack.trackIndex}`][0].popperElm.style.left = this.endPointTrack.target.getBoundingClientRect().left - 20 + 'px'
        this.updateValue()
      }
    },
    dayPlanMouseup(event) {
      if (this.startPointTrack.index !== null) {
        const track = this.weekData[this.startPointTrack.index].data[this.startPointTrack.trackIndex]
        this.trackHandler(this.startPointTrack.index, track.start, track.end)
        this.startPointTrack.index = null
        this.startPointTrack.trackIndex = null
        this.startPointTrack.x = null
        this.startPointTrack.clientWidth = null
        return
      }

      if (this.endPointTrack.index !== null) {
        const track = this.weekData[this.endPointTrack.index].data[this.endPointTrack.trackIndex]
        this.trackHandler(this.endPointTrack.index, track.start, track.end)
        this.endPointTrack.index = null
        this.endPointTrack.trackIndex = null
        this.endPointTrack.x = null
        this.endPointTrack.clientWidth = null
        return
      }
      if (this.tempTrack.index === null) {
        return
      }
      if (this.tempTrack.end - this.tempTrack.start < 10) {
        this.tempTrack.index = null
        this.tempTrack.start = null
        this.tempTrack.end = null
        return
      }
      const index = this.tempTrack.index
      this.weekData[index].data.push({
        start: this.tempTrack.start,
        end: this.tempTrack.end
      })
      this.trackHandler(index, this.tempTrack.start, this.tempTrack.end)
      this.tempTrack.index = null
      this.tempTrack.start = null
      this.tempTrack.end = null
      this.updateValue()
    },
    trackHandler: function(index, start, end) {
      // 检查时间段是否重合 重合则合并
      this.weekData[index].data = this.checkTrack(this.weekData[index].data)
      this.selectedTrack.trackIndex = null
      setTimeout(() => {
        this.selectedTrack.index = index
        for (let i = 0; i < this.weekData[index].data.length; i++) {
          const current = this.weekData[index].data[i]
          if (current.start <= start && current.end >= end) {
            this.selectedTrack.trackIndex = i
            return
          }
        }
      }, 100)
    },
    removeSelectedTrack: function() {
      this.weekData[this.selectedTrack.index].data.splice(this.selectedTrack.trackIndex, 1)
      this.updateValue()
    },
    clearTrack: function() {
      for (let i = 0; i < this.weekData.length; i++) {
        const week = this.weekData[i]
        week.data.splice(0, week.data.length)
      }
      this.updateValue()
    },
    selectAll: function() {
      this.clearTrack()
      for (let i = 0; i < this.weekData.length; i++) {
        const week = this.weekData[i]
        week.data.push({
          start: 0,
          end: 24 * 60
        })
      }
      this.updateValue()
    },
    checkTrack: function(intervals) {
      if (intervals.length === 0) return []

      // 按起始时间排序
      intervals.sort((a, b) => a.start - b.start)

      const merged = [intervals[0]]

      for (let i = 1; i < intervals.length; i++) {
        const current = intervals[i]
        const last = merged[merged.length - 1]

        if (current.start <= last.end) {
          // 合并时间段
          last.end = Math.max(last.end, current.end)
        } else {
          merged.push(current)
        }
      }
      return merged
    },
    updateValue: function() {
      this.$emit('update:planArray', this.weekData)
    },
    weekDataForCopy(index) {
      const result = []
      for (let i = 0; i < this.weekData.length; i++) {
        if (i !== index) {
          result.push({
            weekData: this.weekData[i],
            index: i
          })
        }
      }
      return result
    },
    weekDataCheckBoxForAll: function(index) {
      for (let i = 0; i < this.weekDataCheckBox.length; i++) {
        if (i !== index) {
          this.$set(this.weekDataCheckBox, i, true)
        }
      }
    },
    onSubmitCopy: function(index) {
      const dataValue = this.weekData[index].data
      for (let i = 0; i < this.weekDataCheckBox.length; i++) {
        if (this.weekDataCheckBox[i]) {
          this.$set(this.weekData[i], 'data', JSON.parse(JSON.stringify(dataValue)))
        }
      }

      this.closeCopyBox(index)
    },
    closeCopyBox: function(index) {
      this.weekDataCheckBox = [false, false, false, false, false, false, false]
      this.$refs['copyBox' + index][0].doClose()
    }
  }
}
</script>
<style scoped>
.time-select-header {
  height: 50px;
  line-height: 50px;
  margin-bottom: 20px;
  text-align: right;
}
.time-plan-ruler {
  height: 14px;
  position: relative;
  font-size: 12px;
  line-height: 23px;
}

.time-plan-ruler .hour {
  height: 10px;
}

.time-plan-ruler div {
  display: inline-block;
  height: 5px;
  border-left: 1px solid #555;
}

.time-plan-ruler div:last-child {
  border-right: 1px solid #555;
  border-left: 1px solid #555;
}

.time-plan-ruler .ruler-text {
  position: absolute;
  bottom: 15px;
  margin-left: -5px;
  font: 11px / 1 sans-serif;
}
.time-plan-ruler div:last-child .ruler-text {
  margin-left: 0;
  width: 16px;
}

.time-select-main-container {
  border: 1px solid #e8e8e8;
  box-sizing: border-box;
  margin: 0;
  padding: 9px 0 4px 0;
  overflow: hidden;
}

.time-select-main-container .label {
  line-height: 40px;
  float: left;
  height: 100%;
  padding-left: 10px;
  text-align: center;
}

.time-select-main-container .day-plan {
  position: relative;
  top: 15px;
  height: 12px;
  margin-bottom: 8px;
  border: 1px solid #c5c5c5;
  background-color: #e8eaeb;
  cursor: pointer;
  -webkit-box-sizing: border-box;
  box-sizing: border-box;
}
.time-select-main-container .day-plan .track{
  background: #52c41a;
  position: absolute;
  height: 100%;
}
.time-select-main-container .day-plan .track .hand{
  position: absolute;
  width: 16px;
  height: 16px;
  margin-top: -3px;
  margin-left: -6px;
  background-color: #fff;
  border: solid 2px #91d5ff;
  border-radius: 50%;
}
.time-select-main-container .operate {
  text-align: center;
}

</style>
