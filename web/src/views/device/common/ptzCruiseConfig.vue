<template>
  <div style="height: 100%; display: flex; flex-direction: column;">
    <el-form size="small" inline style="margin-bottom: 12px; padding: 16px 8px; border: 1px solid #e6e6e6; border-radius: 4px;">
      <el-form-item label="巡航组号" style="margin-bottom: 0;">
        <el-input-number v-model="cruiseId" :min="1" :max="255" controls-position="right" style="width: 140px" />
      </el-form-item>
    </el-form>
    <div v-if="presetPoints.length > 0" style="margin-bottom: 8px;">
      <el-tag
        v-for="(item, index) in presetPoints"
        :key="index"
        closable
        size="small"
        style="margin-right: 8px; margin-bottom: 4px;"
        @close="removePoint(item, index)"
      >
        {{ item.presetName || ('预置点' + item.presetId) }}
      </el-tag>
    </div>
    <div v-else style="color: #909399; font-size: 12px; margin-bottom: 8px;">暂无巡航点</div>
    <el-form v-if="showAddPoint" size="mini" inline style="margin-bottom: 8px;">
      <el-form-item style="margin-bottom: 0;">
        <el-select v-model="selectedPreset" placeholder="选择预置点" style="width: 160px">
          <el-option
            v-for="p in allPresetList"
            :key="p.presetId"
            :label="p.presetName || ('预置点' + p.presetId)"
            :value="p"
          />
        </el-select>
      </el-form-item>
      <el-form-item style="margin-bottom: 0;">
        <el-button type="primary" @click="addPoint">确定</el-button>
        <el-button @click="showAddPoint = false">取消</el-button>
      </el-form-item>
    </el-form>
    <el-button v-else size="small" style="margin-bottom: 8px;" @click="showAddPoint = true">添加巡航点</el-button>
    <el-form v-if="showSpeedInput" size="mini" inline style="margin-bottom: 8px;">
      <el-form-item label="速度" style="margin-bottom: 0;">
        <el-input-number v-model="cruiseSpeed" :min="1" :max="255" controls-position="right" style="width: 120px" />
      </el-form-item>
      <el-form-item style="margin-bottom: 0;">
        <el-button type="primary" @click="setSpeed">确定</el-button>
        <el-button @click="cancelSpeed">取消</el-button>
      </el-form-item>
    </el-form>
    <el-button v-else size="small" style="margin-bottom: 8px;" @click="openSpeed">设置巡航速度</el-button>
    <el-form v-if="showTimeInput" size="mini" inline style="margin-bottom: 8px;">
      <el-form-item label="停留时间(秒)" style="margin-bottom: 0;">
        <el-input-number v-model="cruiseTime" :min="1" :max="300" controls-position="right" style="width: 120px" />
      </el-form-item>
      <el-form-item style="margin-bottom: 0;">
        <el-button type="primary" @click="setTime">确定</el-button>
        <el-button @click="cancelTime">取消</el-button>
      </el-form-item>
    </el-form>
    <el-button v-else size="small" style="margin-bottom: 8px;" @click="openTime">设置巡航时间</el-button>
    <div style="margin-top: 8px;">
      <el-button size="small" type="primary" :loading="starting" :disabled="starting" @click="startCruise">开始巡航</el-button>
      <el-button size="small" :loading="stopping" :disabled="stopping" @click="stopCruise">停止巡航</el-button>
      <el-button size="small" type="danger" :loading="deleting" :disabled="deleting" @click="deleteCruise">删除巡航</el-button>
    </div>
  </div>
</template>

<script>
export default {
  name: 'PtzCruiseConfig',
  props: {
    deviceId: { type: String, default: null },
    channelDeviceId: { type: String, default: null }
  },
  data() {
    return {
      cruiseId: 1,
      presetPoints: [],
      allPresetList: [],
      selectedPreset: null,
      showAddPoint: false,
      showSpeedInput: false,
      showTimeInput: false,
      cruiseSpeed: 5,
      cruiseTime: 15,
      starting: false,
      stopping: false,
      deleting: false
    }
  },
  created() {
    this.loadPresets()
  },
  methods: {
    loadPresets() {
      this.$store.dispatch('frontEnd/queryPreset', [this.deviceId, this.channelDeviceId])
        .then(data => {
          this.allPresetList = data || []
        })
        .catch(error => {
          console.log('[巡航] 加载预置点列表失败', error)
        })
    },
    addPoint() {
      if (!this.selectedPreset) {
        this.$message({ showClose: true, message: '请选择预置点', type: 'warning' })
        return
      }
      this.$store.dispatch('frontEnd/addPointForCruise', [this.deviceId, this.channelDeviceId, this.cruiseId, this.selectedPreset.presetId])
        .then(() => {
          this.presetPoints.push(this.selectedPreset)
          this.selectedPreset = null
          this.showAddPoint = false
          this.$message({ showClose: true, message: '添加成功', type: 'success' })
        }).catch(error => {
          this.$message({ showClose: true, message: error, type: 'error' })
        })
    },
    removePoint(preset, index) {
      this.$store.dispatch('frontEnd/deletePointForCruise', [this.deviceId, this.channelDeviceId, this.cruiseId, preset.presetId])
        .then(() => {
          this.presetPoints.splice(index, 1)
          this.$message({ showClose: true, message: '删除成功', type: 'success' })
        }).catch(error => {
          this.$message({ showClose: true, message: error, type: 'error' })
        })
    },
    openSpeed() {
      this.showSpeedInput = true
    },
    cancelSpeed() {
      this.showSpeedInput = false
      this.cruiseSpeed = 5
    },
    setSpeed() {
      this.$store.dispatch('frontEnd/setCruiseSpeed', [this.deviceId, this.channelDeviceId, this.cruiseId, this.cruiseSpeed])
        .then(() => {
          this.showSpeedInput = false
          this.$message({ showClose: true, message: '速度设置成功', type: 'success' })
        }).catch(error => {
          this.$message({ showClose: true, message: error, type: 'error' })
        })
    },
    openTime() {
      this.showTimeInput = true
    },
    cancelTime() {
      this.showTimeInput = false
      this.cruiseTime = 15
    },
    setTime() {
      this.$store.dispatch('frontEnd/setCruiseTime', [this.deviceId, this.channelDeviceId, this.cruiseId, this.cruiseTime])
        .then(() => {
          this.showTimeInput = false
          this.$message({ showClose: true, message: '时间设置成功', type: 'success' })
        }).catch(error => {
          this.$message({ showClose: true, message: error, type: 'error' })
        })
    },
    startCruise() {
      this.starting = true
      this.$store.dispatch('frontEnd/startCruise', [this.deviceId, this.channelDeviceId, this.cruiseId])
        .then(() => {
          this.$message({ showClose: true, message: '巡航启动成功', type: 'success' })
        }).catch(error => {
          this.$message({ showClose: true, message: error, type: 'error' })
        }).finally(() => {
          this.starting = false
        })
    },
    stopCruise() {
      this.stopping = true
      this.$store.dispatch('frontEnd/stopCruise', [this.deviceId, this.channelDeviceId, this.cruiseId])
        .then(() => {
          this.$message({ showClose: true, message: '巡航停止成功', type: 'success' })
        }).catch(error => {
          this.$message({ showClose: true, message: error, type: 'error' })
        }).finally(() => {
          this.stopping = false
        })
    },
    deleteCruise() {
      this.$confirm('确定删除此巡航组所有点?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.deleting = true
        this.$store.dispatch('frontEnd/deletePointForCruise', [this.deviceId, this.channelDeviceId, this.cruiseId, 0])
          .then(() => {
            this.presetPoints = []
            this.$message({ showClose: true, message: '删除成功', type: 'success' })
          }).catch(error => {
            this.$message({ showClose: true, message: error, type: 'error' })
          }).finally(() => {
            this.deleting = false
          })
      }).catch(() => {})
    }
  }
}
</script>
