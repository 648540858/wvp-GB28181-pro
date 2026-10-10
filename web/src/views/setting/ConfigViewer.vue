<template>
  <el-drawer
    :title="title"
    :visible.sync="visible"
    :size="drawerSize"
    :wrapper-closable="true"
    custom-class="config-viewer"
    @closed="handleClosed"
  >
    <div v-loading="loading" class="viewer-body">
      <div class="viewer-toolbar">
        <el-input
          v-model="keyword"
          size="mini"
          clearable
          prefix-icon="el-icon-search"
          placeholder="在文件中查找"
          class="viewer-search"
        />
        <div class="viewer-actions">
          <el-tag v-if="matchCount > 0" size="mini" type="info">{{ matchCount }} 处匹配</el-tag>
          <el-button size="mini" icon="el-icon-refresh" :disabled="loading" @click="reload">刷新</el-button>
          <el-button size="mini" icon="el-icon-document-copy" :disabled="loading || !rawContent" @click="copyContent">复制</el-button>
          <el-button size="mini" icon="el-icon-download" :disabled="loading || !rawContent" @click="downloadContent">下载</el-button>
        </div>
      </div>

      <div v-if="!loading" class="viewer-meta">
        <span class="meta-item"><i class="el-icon-document" /> {{ fileName }}</span>
        <span v-if="profilePath" class="meta-item"><i class="el-icon-folder-opened" /> {{ profilePath }}</span>
        <span class="meta-item"><i class="el-icon-tickets" /> {{ lines.length }} 行</span>
      </div>

      <div class="code-wrapper">
        <div v-if="!loading && lines.length === 0" class="code-empty">文件内容为空</div>
        <div
          v-for="line in lines"
          :key="line.no"
          class="code-line"
          :class="{ 'code-line-hit': line.hit }"
        >
          <span class="code-no">{{ line.no }}</span>
          <span class="code-text">
            <span
              v-for="(seg, idx) in line.segments"
              :key="idx"
              :class="seg.cls"
            >{{ seg.text }}</span>
          </span>
        </div>
      </div>
    </div>

    <div v-if="showFooter" class="viewer-footer">
      <el-button size="mini" @click="visible = false">关闭</el-button>
      <el-button
        v-if="switchable"
        type="primary"
        size="mini"
        :disabled="isActive"
        @click="emitSwitch"
      >
        {{ isActive ? '当前已使用' : '切换到此配置' }}
      </el-button>
    </div>
  </el-drawer>
</template>

<script>
const COMMENT_CLASS = 'tok-comment'
const KEY_CLASS = 'tok-key'
const VALUE_CLASS = 'tok-value'
const DASH_CLASS = 'tok-dash'
const HIT_CLASS = 'tok-hit'

export default {
  name: 'ConfigViewer',
  props: {
    // 是否允许从查看器直接切换配置
    switchable: {
      type: Boolean,
      default: false
    },
    isActive: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      visible: false,
      loading: false,
      keyword: '',
      rawContent: '',
      profileId: '',
      fileName: '',
      profilePath: ''
    }
  },
  computed: {
    drawerSize() {
      return window.innerWidth < 1200 ? '80%' : '60%'
    },
    showFooter() {
      return this.switchable
    },
    title() {
      return this.fileName ? `配置文件 · ${this.fileName}` : '配置文件'
    },
    lines() {
      const raw = this.rawContent || ''
      if (!raw) {
        return []
      }
      const kw = this.keyword.trim()
      const kwLower = kw.toLowerCase()
      return raw.split('\n').map((text, index) => {
        const hit = kw !== '' && text.toLowerCase().indexOf(kwLower) >= 0
        return {
          no: index + 1,
          hit: hit,
          segments: this.buildSegments(text, hit ? kw : '')
        }
      })
    },
    matchCount() {
      return this.lines.filter(line => line.hit).length
    }
  },
  methods: {
    /**
     * 打开查看器
     */
    open(row) {
      this.visible = true
      this.keyword = ''
      this.profileId = row.id
      this.fileName = row.name
      this.profilePath = row.path
      this.load()
    },
    load() {
      this.loading = true
      this.$store.dispatch('setting/getProfileContent', this.profileId)
        .then(data => {
          this.rawContent = data || ''
        })
        .catch(error => {
          this.rawContent = ''
          this.$message.error(typeof error === 'string' ? error : '读取配置文件失败')
        })
        .finally(() => {
          this.loading = false
        })
    },
    reload() {
      this.load()
    },
    handleClosed() {
      this.rawContent = ''
      this.keyword = ''
    },
    emitSwitch() {
      this.$emit('switch', { id: this.profileId, name: this.fileName })
    },
    copyContent() {
      const text = this.rawContent
      const done = () => this.$message.success('已复制到剪贴板')
      if (navigator.clipboard && window.isSecureContext) {
        navigator.clipboard.writeText(text).then(done).catch(() => this.copyByTextarea(text, done))
      } else {
        this.copyByTextarea(text, done)
      }
    },
    copyByTextarea(text, done) {
      const textarea = document.createElement('textarea')
      textarea.value = text
      textarea.style.position = 'fixed'
      textarea.style.opacity = '0'
      document.body.appendChild(textarea)
      textarea.select()
      try {
        document.execCommand('copy')
        done()
      } catch (e) {
        this.$message.error('复制失败，请手动选择内容')
      }
      document.body.removeChild(textarea)
    },
    downloadContent() {
      const blob = new Blob([this.rawContent], { type: 'text/yaml;charset=utf-8' })
      const url = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = this.fileName || 'application.yml'
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      window.URL.revokeObjectURL(url)
    },
    /**
     * 将一行文本拆分为带样式的片段
     * 采用片段对象渲染, 不使用 v-html, 避免配置内容注入标签
     */
    buildSegments(text, keyword) {
      if (text === '') {
        return [{ text: '', cls: '' }]
      }
      const segments = []
      const hashIndex = this.findCommentIndex(text)
      const code = hashIndex >= 0 ? text.slice(0, hashIndex) : text
      const comment = hashIndex >= 0 ? text.slice(hashIndex) : ''

      // 列表项 - xxx
      const dashMatch = /^(\s*)(-\s+)(.*)$/.exec(code)
      let body = code
      if (dashMatch) {
        segments.push({ text: dashMatch[1], cls: '' })
        segments.push({ text: dashMatch[2], cls: DASH_CLASS })
        body = dashMatch[3]
      }

      const kvMatch = /^([^:]+)(:)([\s\S]*)$/.exec(body)
      if (kvMatch && body.indexOf(':') > -1) {
        this.pushToken(segments, kvMatch[1], KEY_CLASS, keyword)
        this.pushToken(segments, kvMatch[2], KEY_CLASS, keyword)
        if (kvMatch[3] !== '') {
          this.pushToken(segments, kvMatch[3], VALUE_CLASS, keyword)
        }
      } else if (body !== '') {
        this.pushToken(segments, body, VALUE_CLASS, keyword)
      }

      if (comment !== '') {
        this.pushToken(segments, comment, COMMENT_CLASS, keyword)
      }
      return segments
    },
    /**
     * 追加片段, 命中关键字时再切分高亮
     */
    pushToken(segments, text, cls, keyword) {
      if (!keyword) {
        segments.push({ text: text, cls: cls })
        return
      }
      const lowerText = text.toLowerCase()
      const lowerKeyword = keyword.toLowerCase()
      let start = 0
      let index = lowerText.indexOf(lowerKeyword, start)
      if (index < 0) {
        segments.push({ text: text, cls: cls })
        return
      }
      while (index >= 0) {
        if (index > start) {
          segments.push({ text: text.slice(start, index), cls: cls })
        }
        segments.push({ text: text.slice(index, index + keyword.length), cls: cls + ' ' + HIT_CLASS })
        start = index + keyword.length
        index = lowerText.indexOf(lowerKeyword, start)
      }
      if (start < text.length) {
        segments.push({ text: text.slice(start), cls: cls })
      }
    },
    /**
     * 查找注释起始位置(引号内的 # 不算注释)
     */
    findCommentIndex(text) {
      let inSingleQuote = false
      let inDoubleQuote = false
      for (let i = 0; i < text.length; i++) {
        const ch = text[i]
        if (ch === "'" && !inDoubleQuote) {
          inSingleQuote = !inSingleQuote
        } else if (ch === '"' && !inSingleQuote) {
          inDoubleQuote = !inDoubleQuote
        } else if (ch === '#' && !inSingleQuote && !inDoubleQuote) {
          return i
        }
      }
      return -1
    }
  }
}
</script>

<style scoped>
.viewer-body {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 0 16px 16px;
  box-sizing: border-box;
}

.viewer-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 12px;
}

.viewer-search {
  width: 260px;
}

.viewer-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.viewer-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  padding: 8px 12px;
  margin-bottom: 8px;
  background: #f7f8fa;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  font-size: 12px;
  color: #606266;
}

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  word-break: break-all;
}

.code-wrapper {
  flex: 1;
  overflow: auto;
  background: #fbfbfc;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 8px 0;
  font-family: Menlo, Monaco, Consolas, 'Courier New', monospace;
  font-size: 12px;
  line-height: 20px;
}

.code-line {
  display: flex;
  white-space: pre;
}

.code-line-hit {
  background: #fff8e1;
}

.code-no {
  flex: 0 0 56px;
  width: 56px;
  padding-right: 12px;
  text-align: right;
  color: #b4b8c2;
  user-select: none;
}

.code-text {
  flex: 1;
  padding-right: 12px;
  color: #2f3542;
}

.code-empty {
  padding: 24px;
  text-align: center;
  color: #909399;
}

.tok-comment {
  color: #8d9aa8;
  font-style: italic;
}

.tok-key {
  color: #1f6feb;
}

.tok-value {
  color: #b5561a;
}

.tok-dash {
  color: #8250df;
}

.tok-hit {
  background: #ffe58f;
  border-radius: 2px;
}

.viewer-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 12px 16px;
  border-top: 1px solid #ebeef5;
}
</style>
