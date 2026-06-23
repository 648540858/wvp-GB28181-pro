<template>
  <div>
    <el-form label-width="120px" class="guard-form">
      <el-form-item label="启用">
        <el-switch v-model="enabled" />
      </el-form-item>
      <el-form-item label="预置位">
        <el-select v-model="presetIndex" style="width: 180px" placeholder="选择预置位">
          <el-option v-for="p in allPresetList" :key="p.presetId"
                     :label="p.presetId + '-' + (p.presetName || ('预置点' + p.presetId))"
                     :value="Number(p.presetId)" />
        </el-select>
      </el-form-item>
      <el-form-item label="自动归位（秒）">
        <el-input-number v-model="resetTime" :min="1" :max="999999" controls-position="right" style="width: 180px" />
      </el-form-item>
      <el-form-item>
        <div class="guard-actions">
          <el-button @click="loadPresets">刷新</el-button>
          <el-button type="primary" :loading="submitting" :disabled="submitting" @click="confirmSave">保存</el-button>
        </div>
      </el-form-item>
    </el-form>
  </div>
</template>

<script>
export default {
  name: 'ChPtzGuardConfig',
  props: {
    channelId: { type: String, default: null }
  },
  data() {
    return {
      enabled: false,
      presetIndex: null,
      resetTime: 10,
      allPresetList: [],
      submitting: false
    }
  },
  created() {
    this.loadPresets()
  },
  methods: {
    loadPresets() {
      this.$store.dispatch('commonChanel/queryPreset', this.channelId)
        .then(data => {
          this.allPresetList = data || []
        })
        .catch(error => {
          console.log('[看守位] 加载预置点列表失败', error)
        })
    },
    confirmSave() {
      if (!this.enabled && !this.presetIndex) {
        this.$message({ showClose: true, message: '请选择预置位编号', type: 'warning' })
        return
      }
      if (this.resetTime == null || this.resetTime < 1) {
        this.$message({ showClose: true, message: '请输入有效的归位时间', type: 'warning' })
        return
      }
      this.submitting = true
      const params = {
        channelId: this.channelId,
        enabled: this.enabled
      }
      if (this.presetIndex != null) {
        params.presetIndex = this.presetIndex
      }
      if (this.resetTime != null) {
        params.resetTime = this.resetTime
      }
      this.$store.dispatch('commonChanel/homePosition', params)
        .then(() => {
          this.$message({ showClose: true, message: '保存成功', type: 'success' })
        })
        .catch(error => {
          this.$message({ showClose: true, message: error || '保存失败', type: 'error' })
        })
        .finally(() => {
          this.submitting = false
        })
    }
  }
}
</script>

<style scoped>
.guard-form {
  padding: 16px 12px;
  max-width: 420px;
}
.guard-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>
