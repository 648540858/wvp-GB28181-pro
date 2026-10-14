<template>
  <div class="tag-list-input">
    <div v-if="list.length" class="tag-list">
      <el-tag
        v-for="(item, index) in list"
        :key="item"
        class="tag-item"
        size="small"
        :closable="!disabled"
        :disable-transitions="true"
        @close="handleRemove(index)"
      >
        {{ item }}
      </el-tag>
    </div>
    <div v-else class="tag-empty">暂无配置</div>

    <div class="tag-input-row">
      <el-input
        v-model="draft"
        :disabled="disabled"
        :placeholder="placeholder"
        class="tag-input"
        @keyup.enter.native="handleAdd"
        @paste.native="handlePaste"
      />
      <el-button size="small" :disabled="disabled || !draft.trim()" @click="handleAdd">添加</el-button>
    </div>

    <div class="tag-hint">
      <span v-if="error" class="tag-error">{{ error }}</span>
      <span v-else-if="tips" class="tag-tips">{{ tips }}</span>
      <span class="tag-count">共 {{ list.length }} 条</span>
    </div>
  </div>
</template>

<script>
/**
 * 标签式列表输入控件
 * <p>
 * 每个配置项以标签展示, 可单条删除; 输入框回车/点击按钮/粘贴多条即可新增,
 * 支持去重与按 itemValidator 做单项格式校验。
 */
export default {
  name: 'TagListInput',
  props: {
    value: {
      type: Array,
      default: () => []
    },
    disabled: {
      type: Boolean,
      default: false
    },
    placeholder: {
      type: String,
      default: '输入后回车或点击添加'
    },
    tips: {
      type: String,
      default: ''
    },
    // 单项校验函数, 返回错误文案(字符串)表示不通过, 返回空字符串表示通过
    itemValidator: {
      type: Function,
      default: null
    }
  },
  data() {
    return {
      draft: '',
      error: ''
    }
  },
  computed: {
    list() {
      return Array.isArray(this.value) ? this.value : []
    }
  },
  methods: {
    validate(raw) {
      if (this.list.includes(raw)) {
        return `"${raw}" 已存在`
      }
      if (typeof this.itemValidator === 'function') {
        return this.itemValidator(raw) || ''
      }
      return ''
    },
    emit(list) {
      this.$emit('input', list)
      this.$emit('change', list)
    },
    handleAdd() {
      const raw = this.draft.trim()
      if (!raw) {
        return
      }
      const message = this.validate(raw)
      if (message) {
        this.error = message
        return
      }
      this.emit([...this.list, raw])
      this.draft = ''
      this.error = ''
    },
    handleRemove(index) {
      const next = this.list.slice()
      next.splice(index, 1)
      this.emit(next)
      this.error = ''
    },
    // 支持一次粘贴多条(按换行/中英文逗号拆分)
    handlePaste(event) {
      const clipboard = event.clipboardData || window.clipboardData
      const text = clipboard ? clipboard.getData('text') : ''
      if (!text || !/[\n,，]/.test(text)) {
        return
      }
      event.preventDefault()
      const next = this.list.slice()
      let message = ''
      text.split(/[\n,，]/).forEach(item => {
        const raw = (item || '').trim()
        if (!raw) {
          return
        }
        if (next.includes(raw)) {
          message = `"${raw}" 已存在`
          return
        }
        const invalid = typeof this.itemValidator === 'function' ? this.itemValidator(raw) : ''
        if (invalid) {
          message = invalid
          return
        }
        next.push(raw)
      })
      if (next.length !== this.list.length) {
        this.emit(next)
      }
      this.draft = ''
      this.error = message
    }
  }
}
</script>

<style scoped>
.tag-list-input {
  display: inline-block;
  width: 480px;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 8px;
}

.tag-item {
  max-width: 100%;
}

.tag-empty {
  margin-bottom: 8px;
  font-size: 12px;
  color: #c0c4cc;
}

.tag-input-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.tag-input {
  flex: 1;
}

.tag-hint {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 4px;
  font-size: 12px;
  line-height: 18px;
  min-height: 18px;
}

.tag-error {
  color: #f56c6c;
}

.tag-tips {
  color: #909399;
}

.tag-count {
  margin-left: auto;
  color: #c0c4cc;
}
</style>
