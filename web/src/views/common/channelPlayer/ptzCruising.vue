<template>
  <div id="ptzCruising">
    <div style="display: grid; grid-template-columns: 80px auto; line-height: 28px">
      <span>巡航组号: </span>
      <el-input
        v-model="tourId"
        min="1"
        max="255"
        placeholder="巡航组号"
        addon-before="巡航组号"
        addon-after="(1-255)"
        size="mini"
      />
    </div>
    <p>
      <el-tag
        v-for="(item, index) in presetList"
        :key="item.presetId"
        closable
        style="margin-right: 1rem; cursor: pointer"
        @close="delPreset(item, index)"
      >
        {{ item.presetName ? item.presetName : item.presetId }}
      </el-tag>
    </p>

    <el-form v-if="selectPresetVisible" size="mini" :inline="true">
      <el-form-item>
        <el-select v-model="selectPreset" placeholder="请选择预置点">
          <el-option
            v-for="item in allPresetList"
            :key="item.presetId"
            :label="item.presetName"
            :value="item"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="addCruisePoint">保存</el-button>
        <el-button type="primary" @click="cancelAddCruisePoint">取消</el-button>
      </el-form-item>
    </el-form>
    <el-button v-else size="mini" @click="selectPresetVisible=true">添加巡航点</el-button>

    <el-form v-if="setSpeedVisible" size="mini" :inline="true">
      <el-form-item>
        <el-input
          v-if="setSpeedVisible"
          v-model="cruiseSpeed"
          min="1"
          max="4095"
          placeholder="巡航速度"
          addon-before="巡航速度"
          addon-after="(1-4095)"
          size="mini"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="setCruiseSpeed">保存</el-button>
        <el-button @click="cancelSetCruiseSpeed">取消</el-button>
      </el-form-item>
    </el-form>
    <el-button v-else size="mini" @click="setSpeedVisible = true">设置巡航速度</el-button>
    <el-form v-if="setTimeVisible" size="mini" :inline="true">
      <el-form-item>
        <el-input
          v-model="cruiseTime"
          min="1"
          max="4095"
          placeholder="巡航停留时间(秒)"
          addon-before="巡航停留时间(秒)"
          addon-after="(1-4095)"
          style="width: 100%;"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="setCruiseTime">保存</el-button>
        <el-button @click="cancelSetCruiseTime">取消</el-button>
      </el-form-item>
    </el-form>
    <el-button v-else size="mini" @click="setTimeVisible = true">设置巡航时间</el-button>
    <el-button size="mini" @click="startCruise">开始巡航</el-button>
    <el-button size="mini" @click="stopCruise">停止巡航</el-button>
    <el-button size="mini" type="danger" @click="deleteCruise">删除巡航</el-button>
  </div>
</template>

<script>

export default {
  name: 'PtzCruising',
  components: {},
  props: ['channelId'],
  data() {
    return {
      tourId: 1,
      presetList: [],
      allPresetList: [],
      selectPreset: '',
      inputVisible: false,
      selectPresetVisible: false,
      setSpeedVisible: false,
      setTimeVisible: false,
      cruiseSpeed: '',
      cruiseTime: ''
    }
  },
  created() {
    this.getPresetList()
  },
  methods: {
    getPresetList: function() {
      this.$store.dispatch('commonChanel/queryPreset', this.channelId)
        .then((data) => {
          this.allPresetList = data
        })
    },
    addCruisePoint: function() {
      const loading = this.$loading({
        lock: true,
        fullscreen: true,
        text: '正在发送指令',
        spinner: 'el-icon-loading',
        background: 'rgba(0, 0, 0, 0.7)'
      })
      this.$store.dispatch('commonChanel/addPointForCruise',
        {
          channelId: this.channelId,
          tourId: this.tourId,
          presetId: this.selectPreset.presetId
        })
        .then((data) => {
          this.presetList.push(this.selectPreset)
        }).catch((error) => {
          this.$message({
            showClose: true,
            message: error,
            type: 'error'
          })
        }).finally(() => {
          this.selectPreset = ''
          this.selectPresetVisible = false
          loading.close()
        })
    },
    cancelAddCruisePoint: function() {
      this.selectPreset = ''
      this.selectPresetVisible = false
    },
    delPreset: function(preset, index) {
      const loading = this.$loading({
        lock: true,
        fullscreen: true,
        text: '正在发送指令',
        spinner: 'el-icon-loading',
        background: 'rgba(0, 0, 0, 0.7)'
      })
      this.$store.dispatch('commonChanel/deletePointForCruise',
        {
          channelId: this.channelId,
          tourId: this.tourId,
          presetId: preset.presetId
        })
        .then((data) => {
          this.presetList.splice(index, 1)
        }).catch((error) => {
          this.$message({
            showClose: true,
            message: error,
            type: 'error'
          })
        }).finally(() => {
          loading.close()
        })
    },
    deleteCruise: function(preset, index) {
      this.$confirm('确定删除此巡航组', '提示', {
        dangerouslyUseHTMLString: true,
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        const loading = this.$loading({
          lock: true,
          fullscreen: true,
          text: '正在发送指令',
          spinner: 'el-icon-loading',
          background: 'rgba(0, 0, 0, 0.7)'
        })
        this.$store.dispatch('commonChanel/deletePointForCruise',
          {
            channelId: this.channelId,
            tourId: this.tourId,
            presetId: 0
          })
          .then((data) => {
            this.presetList = []
          }).catch((error) => {
            this.$message({
              showClose: true,
              message: error,
              type: 'error'
            })
          }).finally(() => {
            loading.close()
          })
      })
    },
    setCruiseSpeed: function() {
      if (this.presetList.length === 0) {
        this.$message({
          showClose: true,
          message: '请添加巡航点',
          type: 'warning'
        })
        return
      }
      const loading = this.$loading({
        lock: true,
        fullscreen: true,
        text: '正在发送指令',
        spinner: 'el-icon-loading',
        background: 'rgba(0, 0, 0, 0.7)'
      })
      this.$store.dispatch('commonChanel/setCruiseSpeed',
        {
          channelId: this.channelId,
          tourId: this.tourId,
          presetId: this.presetList.at(-1).presetId,
          speed: this.cruiseSpeed
        })
        .then((data) => {
          this.$message({
            showClose: true,
            message: '保存成功',
            type: 'success'
          })
        }).catch((error) => {
          this.$message({
            showClose: true,
            message: error,
            type: 'error'
          })
        }).finally(() => {
          this.cruiseSpeed = ''
          this.setSpeedVisible = false
          loading.close()
        })
    },
    cancelSetCruiseSpeed: function() {
      this.cruiseSpeed = ''
      this.setSpeedVisible = false
    },
    setCruiseTime: function() {
      if (this.presetList.length === 0) {
        this.$message({
          showClose: true,
          message: '请添加巡航点',
          type: 'warning'
        })
        return
      }
      const loading = this.$loading({
        lock: true,
        fullscreen: true,
        text: '正在发送指令',
        spinner: 'el-icon-loading',
        background: 'rgba(0, 0, 0, 0.7)'
      })
      this.$store.dispatch('commonChanel/setCruiseTime',
        {
          channelId: this.channelId,
          tourId: this.tourId,
          time: this.cruiseTime,
          presetId: this.presetList.at(-1).presetId
        })
        .then((data) => {
          this.$message({
            showClose: true,
            message: '保存成功',
            type: 'success'
          })
        }).catch((error) => {
          this.$message({
            showClose: true,
            message: error,
            type: 'error'
          })
        }).finally(() => {
          this.setTimeVisible = false
          this.cruiseTime = ''
          loading.close()
        })
    },
    cancelSetCruiseTime: function() {
      this.setTimeVisible = false
      this.cruiseTime = ''
    },
    startCruise: function() {
      const loading = this.$loading({
        lock: true,
        fullscreen: true,
        text: '正在发送指令',
        spinner: 'el-icon-loading',
        background: 'rgba(0, 0, 0, 0.7)'
      })
      this.$store.dispatch('commonChanel/startCruise',
        {
          channelId: this.channelId,
          tourId: this.tourId
        })
        .then((data) => {
          this.$message({
            showClose: true,
            message: '发送成功',
            type: 'success'
          })
        }).catch((error) => {
          this.$message({
            showClose: true,
            message: error,
            type: 'error'
          })
        }).finally(() => {
          this.setTimeVisible = false
          this.cruiseTime = ''
          loading.close()
        })
    },
    stopCruise: function() {
      const loading = this.$loading({
        lock: true,
        fullscreen: true,
        text: '正在发送指令',
        spinner: 'el-icon-loading',
        background: 'rgba(0, 0, 0, 0.7)'
      })
      this.$store.dispatch('commonChanel/stopCruise',
        {
          channelId: this.channelId,
          tourId: this.tourId
        })
        .then((data) => {
          this.$message({
            showClose: true,
            message: '发送成功',
            type: 'success'
          })
        }).catch((error) => {
          this.$message({
            showClose: true,
            message: error,
            type: 'error'
          })
        }).finally(() => {
          this.setTimeVisible = false
          this.cruiseTime = ''
          loading.close()
        })
    }
  }
}
</script>
