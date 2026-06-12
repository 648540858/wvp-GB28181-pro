<template>
  <div id="ptzScanConfig" style="height: 100%; display: flex; flex-direction: column;">
    <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 6px;">
      <div>
        <el-button type="primary" :loading="adding" :disabled="adding" @click="addLineScan">添加线扫</el-button>
        <el-button @click="clearAll">清空</el-button>
      </div>
      <el-button icon="el-icon-refresh-right" circle />
    </div>
    <div v-if="scanAreas.length > 0" style="flex: 1; overflow: auto;">
      <el-table :data="scanAreas" max-height="100%" stripe border highlight-current-row height="100%">
        <el-table-column label="序号" min-width="50">
          <template v-slot="{ row }">{{ row.index }}</template>
        </el-table-column>
        <el-table-column label="名称" min-width="80">
          <template v-slot="{ row }">{{ row.name }}</template>
        </el-table-column>
        <el-table-column label="左边界" min-width="90">
          <template v-slot="{ row }">
            <el-button type="text"
                       :style="{ color: row.leftBoundary ? '#67C23A' : '#409EFF' }"
                       :loading="boundaryLoading.index === row.index && boundaryLoading.side === 'Left'"
                       :disabled="operatingId !== null"
                       @click="setBoundary(row, 'Left')">
              {{ row.leftBoundary ? '重新保存' : '待保存' }}
            </el-button>
          </template>
        </el-table-column>
        <el-table-column label="右边界" min-width="90">
          <template v-slot="{ row }">
            <el-button type="text"
                       :style="{ color: row.rightBoundary ? '#67C23A' : '#409EFF' }"
                       :loading="boundaryLoading.index === row.index && boundaryLoading.side === 'Right'"
                       :disabled="operatingId !== null"
                       @click="setBoundary(row, 'Right')">
              {{ row.rightBoundary ? '重新保存' : '待保存' }}
            </el-button>
          </template>
        </el-table-column>
        <el-table-column label="速度" min-width="90">
          <template v-slot="{ row }">
            <el-select v-model="row.speed" :disabled="speedSaving === row.index" @change="onSpeedChange(row)">
              <el-option v-for="s in 8" :key="s" :label="s" :value="s" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="120">
          <template v-slot="{ row, $index }">
            <el-button v-if="$index === cruisingScanIndex" type="text" style="color: #F56C6C" :loading="operatingId === row.index" :disabled="operatingId !== null" @click="stopScan(row)">停用</el-button>
            <el-button v-else type="text" style="color: #409EFF" :disabled="operatingId !== null" :loading="operatingId === row.index" @click="startScan(row, $index)">启用</el-button>
            <el-button type="text" style="color: #F56C6C" :disabled="operatingId !== null" @click="deleteScan(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
    <div v-else style="color: #909399; font-size: 12px; margin-bottom: 8px;">暂无线扫区域</div>
  </div>
</template>

<script>
export default {
  name: 'PtzScanConfig',
  props: {
    deviceId: { type: String, default: null },
    channelDeviceId: { type: String, default: null }
  },
  data() {
    return {
      scanAreas: [],
      cruisingScanIndex: null,
      operatingId: null,
      adding: false,
      boundaryLoading: { index: null, side: null },
      speedSaving: null
    }
  },
  methods: {
    getNextAvailableIndex() {
      const used = new Set(this.scanAreas.filter(a => a.name && a.name.trim()).map(a => a.index))
      for (let i = 0; i <= 255; i++) {
        if (!used.has(i)) return i
      }
      return 0
    },
    addLineScan() {
      const nextIndex = this.getNextAvailableIndex()
      const name = '线扫' + nextIndex
      this.adding = true
      this.scanAreas.push({
        index: nextIndex,
        name: name,
        leftBoundary: false,
        rightBoundary: false,
        speed: 5
      })
      this.$nextTick(() => { this.adding = false })
    },
    setBoundary(row, boundary) {
      this.boundaryLoading = { index: row.index, side: boundary }
      const action = boundary === 'Left' ? 'setLeftForScan' : 'setRightForScan'
      this.$store.dispatch('frontEnd/' + action, [this.deviceId, this.channelDeviceId, row.index])
        .then(() => {
          this.$message({ showClose: true, message: (boundary === 'Left' ? '左' : '右') + '边界设置成功', type: 'success' })
          if (boundary === 'Left') {
            row.leftBoundary = true
          } else {
            row.rightBoundary = true
          }
        }).catch(() => {
          this.$message({ showClose: true, message: '边界设置失败', type: 'error' })
        }).finally(() => {
          this.boundaryLoading = { index: null, side: null }
        })
    },
    onSpeedChange(row) {
      this.speedSaving = row.index
      this.$store.dispatch('frontEnd/setSpeedForScan', [this.deviceId, this.channelDeviceId, row.index, row.speed])
        .then(() => {
          this.$message({ showClose: true, message: '速度已保存', type: 'success' })
        }).catch(() => {
          this.$message({ showClose: true, message: '速度保存失败', type: 'error' })
        }).finally(() => {
          this.speedSaving = null
        })
    },
    startScan(row, index) {
      this.operatingId = row.index
      this.$store.dispatch('frontEnd/startScan', [this.deviceId, this.channelDeviceId, row.index])
        .then(() => {
          this.$message({ showClose: true, message: '启用成功', type: 'success' })
          this.cruisingScanIndex = index
        }).catch(() => {
          this.$message({ showClose: true, message: '启用失败', type: 'error' })
        }).finally(() => {
          this.operatingId = null
        })
    },
    stopScan(row) {
      this.operatingId = row.index
      this.$store.dispatch('frontEnd/stopScan', [this.deviceId, this.channelDeviceId, row.index])
        .then(() => {
          this.$message({ showClose: true, message: '停用成功', type: 'success' })
          this.cruisingScanIndex = null
        }).catch(() => {
          this.$message({ showClose: true, message: '停用失败', type: 'error' })
        }).finally(() => {
          this.operatingId = null
        })
    },
    deleteScan(row) {
      this.$confirm('确定删除线扫 ' + row.index + '?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        const idx = this.scanAreas.indexOf(row)
        if (idx !== -1) this.scanAreas.splice(idx, 1)
        if (this.cruisingScanIndex !== null && this.scanAreas[this.cruisingScanIndex] === undefined) {
          this.cruisingScanIndex = null
        }
        this.$message({ showClose: true, message: '删除成功（仅本地列表，设备端配置需手动清除）', type: 'success' })
      }).catch(() => {})
    },
    clearAll() {
      if (this.scanAreas.length === 0) return
      this.$confirm('确定清空所有线扫区域?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.scanAreas = []
        this.cruisingScanIndex = null
        this.$message({ showClose: true, message: '清空成功（仅本地列表，设备端配置需手动清除）', type: 'success' })
      }).catch(() => {})
    }
  }
}
</script>
