<template>
  <div style="height: 100%; display: flex; flex-direction: column;">
    <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 6px;">
      <div>
        <el-button type="primary" :disabled="showAddForm" @click="openAdd">添加预置点</el-button>
        <el-button :loading="clearing" :disabled="clearing" @click="clearAll">清空</el-button>
      </div>
      <el-button icon="el-icon-refresh-right" circle @click="getPresetList" />
    </div>
    <el-form v-if="showAddForm" size="small" inline style="margin-bottom: 6px; padding: 16px 8px; border: 1px solid #e6e6e6; border-radius: 4px; display: flex; align-items: center;">
      <el-form-item label="序号" style="margin-bottom: 0; margin-right: 2rem">
        <el-input-number v-model="addPresetId" :min="1" :max="255" controls-position="right" style="width: 180px" />
      </el-form-item>
      <el-form-item style="margin-bottom: 0;">
        <el-button type="primary" :loading="submitting" :disabled="submitting" @click="confirmAdd">确定</el-button>
        <el-button @click="cancelAdd">取消</el-button>
      </el-form-item>
    </el-form>
    <el-table
      :data="presetList"
      border
      stripe
      max-height="100%"
      style="flex: 1"
    >
      <el-table-column prop="presetId" label="序号" align="center" />
      <el-table-column label="名称">
        <template v-slot="{ row }">
          <span>{{ row.presetName || ('预置点' + row.presetId) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" min-width="140" align="center">
        <template v-slot="{ row }">
          <el-button size="mini" type="text" @click="callPreset(row)">调用</el-button>
          <el-button size="mini" type="text" style="color: #F56C6C" :loading="deletingId === row.presetId" :disabled="deletingId !== null" @click="delPreset(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script>
export default {
  name: 'ChPtzPresetConfig',
  props: {
    channelId: { type: String, default: null }
  },
  data() {
    return {
      presetList: [],
      showAddForm: false,
      addPresetId: 1,
      submitting: false,
      clearing: false,
      deletingId: null
    }
  },
  created() {
    this.getPresetList()
  },
  methods: {
    getPresetList() {
      this.$store.dispatch('commonChanel/queryPreset', this.channelId)
        .then(data => {
          this.presetList = data || []
        })
        .catch(error => {
          console.log(error)
        })
    },
    openAdd() {
      this.addPresetId = this.getNextAvailableId()
      this.showAddForm = true
    },
    cancelAdd() {
      this.showAddForm = false
      this.addPresetId = 1
    },
    confirmAdd() {
      const exists = this.presetList.some(p => p.presetId === this.addPresetId)
      if (exists) {
        this.$message({ showClose: true, message: '序号 ' + this.addPresetId + ' 已存在', type: 'warning' })
        return
      }
      this.submitting = true
      this.$store.dispatch('commonChanel/addPreset', { channelId: this.channelId, presetId: this.addPresetId, presetName: '' })
        .then(() => {
          this.showAddForm = false
          setTimeout(() => {
            this.getPresetList()
          }, 600)
        }).catch(error => {
          this.$message({ showClose: true, message: error, type: 'error' })
        }).finally(() => {
          this.submitting = false
        })
    },
    callPreset(preset) {
      this.$store.dispatch('commonChanel/callPreset', { channelId: this.channelId, presetId: preset.presetId })
        .then(() => {
          this.$message({ showClose: true, message: '调用成功', type: 'success' })
        }).catch(error => {
          this.$message({ showClose: true, message: error, type: 'error' })
        })
    },
    delPreset(preset) {
      this.$confirm('确定删除此预置位', '提示', {
        dangerouslyUseHTMLString: true,
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.deletingId = preset.presetId
        this.$store.dispatch('commonChanel/deletePreset', { channelId: this.channelId, presetId: preset.presetId })
          .then(() => {
            this.getPresetList()
          }).catch(error => {
            this.$message({ showClose: true, message: error, type: 'error' })
          }).finally(() => {
            this.deletingId = null
          })
      }).catch(() => {})
    },
    clearAll() {
      if (this.presetList.length === 0) return
      this.$confirm('确定清空所有预置点?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.clearing = true
        const promises = this.presetList.map(p =>
          this.$store.dispatch('commonChanel/deletePreset', { channelId: this.channelId, presetId: p.presetId })
        )
        Promise.all(promises).then(() => {
          this.presetList = []
          this.$message({ showClose: true, message: '清空成功', type: 'success' })
        }).catch(() => {
          this.$message({ showClose: true, message: '清空失败', type: 'error' })
        }).finally(() => {
          this.clearing = false
        })
      }).catch(() => {})
    },
    getNextAvailableId() {
      if (!this.presetList || this.presetList.length === 0) return 1
      const used = this.presetList.map(p => Number(p.presetId)).sort((a, b) => a - b)
      for (let i = 0; i < used.length - 1; i++) {
        if (used[i + 1] - used[i] > 1) return used[i] + 1
      }
      return used[used.length - 1] + 1
    }
  }
}
</script>
