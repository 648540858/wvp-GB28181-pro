<template>
  <el-drawer
    :title="title"
    :visible.sync="visible"
    :size="drawerSize"
    :wrapper-closable="true"
    custom-class="config-viewer"
    :before-close="handleBeforeClose"
    @closed="handleClosed"
  >
    <div v-loading="loading" class="viewer-body">
      <div class="viewer-toolbar">
        <el-input
          v-if="!editing"
          v-model="keyword"
          size="mini"
          clearable
          prefix-icon="el-icon-search"
          placeholder="在文件中查找"
          class="viewer-search"
        />
        <div v-else class="viewer-editing-tip">
          <i class="el-icon-edit-outline" /> 编辑模式 · {{ fileName }}
        </div>
        <div class="viewer-actions">
          <el-tag v-if="!editing && matchCount > 0" size="mini" type="info">{{ matchCount }} 处匹配</el-tag>
          <el-button
            v-if="!editing && detail.editable"
            size="mini"
            icon="el-icon-edit"
            :disabled="loading"
            @click="startEdit"
          >
            编辑
          </el-button>
          <el-button v-if="!editing" size="mini" icon="el-icon-refresh" :disabled="loading" @click="reload">刷新</el-button>
          <el-button
            v-if="!editing"
            size="mini"
            icon="el-icon-document-copy"
            :disabled="loading || !rawContent"
            @click="copyContent"
          >
            复制
          </el-button>
          <el-button
            v-if="!editing"
            size="mini"
            icon="el-icon-download"
            :disabled="loading || !rawContent"
            @click="downloadContent"
          >
            下载
          </el-button>
          <template v-if="editing">
            <el-button size="mini" icon="el-icon-refresh-left" :disabled="!dirty" @click="resetEdit">还原</el-button>
            <el-button size="mini" icon="el-icon-close" @click="cancelEdit">取消</el-button>
            <el-button
              type="primary"
              size="mini"
              icon="el-icon-check"
              :loading="saving"
              @click="saveContent"
            >
              保存
            </el-button>
          </template>
        </div>
      </div>

      <div v-if="!loading" class="viewer-meta">
        <span class="meta-item"><i class="el-icon-document" /> {{ fileName }}</span>
        <span v-if="profilePath" class="meta-item"><i class="el-icon-folder-opened" /> {{ profilePath }}</span>
        <span class="meta-item"><i class="el-icon-tickets" /> {{ lines.length }} 行</span>
        <span v-if="detail.active" class="meta-item"><el-tag size="mini" type="success">使用中</el-tag></span>
      </div>

      <el-alert
        v-if="editing && detail.mainConfig"
        type="info"
        :closable="false"
        show-icon
        title="主配置文件在线编辑不支持修改 spring.profiles.active，如需切换配置请使用「切换到此配置」功能。"
        class="viewer-alert"
      />
      <el-alert
        v-if="saveError"
        type="error"
        :closable="true"
        show-icon
        :title="saveError"
        class="viewer-alert"
        @close="saveError = ''"
      />

      <div v-if="!editing" class="code-wrapper">
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

      <div v-else class="code-editor-wrapper">
        <codemirror
          ref="editor"
          v-model="draft"
          :options="editorOptions"
          class="code-editor"
        />
      </div>
    </div>

    <div v-if="showFooter" class="viewer-footer">
      <template v-if="editing">
        <span class="footer-tip">
          <template v-if="dirty">有未保存的修改</template>
          <template v-else>内容未修改</template>
        </span>
      </template>
      <template v-else>
        <el-button size="mini" @click="visible = false">关闭</el-button>
        <el-button
          v-if="switchable && !detail.mainConfig"
          type="primary"
          size="mini"
          :disabled="isActive"
          @click="emitSwitch"
        >
          {{ isActive ? '当前已使用' : '切换到此配置' }}
        </el-button>
      </template>
    </div>
  </el-drawer>
</template>

<script>
import { codemirror } from 'vue-codemirror'
import 'codemirror/lib/codemirror.css'
import 'codemirror/theme/eclipse.css'
import 'codemirror/mode/yaml/yaml'
import 'codemirror/addon/edit/matchbrackets'

const COMMENT_CLASS = 'tok-comment'
const KEY_CLASS = 'tok-key'
const VALUE_CLASS = 'tok-value'
const DASH_CLASS = 'tok-dash'
const HIT_CLASS = 'tok-hit'

export default {
  name: 'ConfigViewer',
  components: { codemirror },
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
      saving: false,
      keyword: '',
      rawContent: '',
      draft: '',
      originalContent: '',
      editing: false,
      saveError: '',
      profileId: '',
      fileName: '',
      profilePath: '',
      detail: {
        writable: false,
        editable: false,
        active: false,
        mainConfig: false,
        contentHash: ''
      },
      editorOptions: {
        mode: 'text/x-yaml',
        theme: 'eclipse',
        lineNumbers: true,
        lineWrapping: false,
        indentUnit: 2,
        tabSize: 2,
        matchBrackets: true,
        autofocus: false
      }
    }
  },
  computed: {
    drawerSize() {
      return window.innerWidth < 1200 ? '80%' : '60%'
    },
    showFooter() {
      return this.detail.editable || this.switchable
    },
    title() {
      return this.fileName ? `配置文件 · ${this.fileName}` : '配置文件'
    },
    dirty() {
      return this.editing && this.draft !== this.originalContent
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
      this.editing = false
      this.draft = ''
      this.originalContent = ''
      this.saveError = ''
      this.load()
    },
    load() {
      this.loading = true
      this.$store.dispatch('setting/getProfileDetail', this.profileId)
        .then(data => {
          if (!data) {
            return
          }
          this.rawContent = data.content || ''
          this.draft = this.rawContent
          this.originalContent = this.rawContent
          this.fileName = data.fileName || this.fileName
          this.profilePath = data.path || ''
          this.detail = {
            writable: !!data.writable,
            editable: !!data.editable,
            active: !!data.active,
            mainConfig: !!data.mainConfig,
            contentHash: data.contentHash || ''
          }
          this.saveError = ''
        })
        .catch(error => {
          this.rawContent = ''
          this.draft = ''
          this.$message.error(typeof error === 'string' ? error : '读取配置文件失败')
        })
        .finally(() => {
          this.loading = false
        })
    },
    reload() {
      this.confirmDiscard().then(ok => {
        if (ok) {
          this.editing = false
          this.load()
        }
      })
    },
    startEdit() {
      this.draft = this.rawContent
      this.originalContent = this.rawContent
      this.editing = true
      this.saveError = ''
      this.$nextTick(() => {
        const editor = this.$refs.editor
        if (editor && editor.codemirror) {
          editor.codemirror.focus()
        }
      })
    },
    resetEdit() {
      this.confirmReset().then(ok => {
        if (ok) {
          this.draft = this.originalContent
          this.saveError = ''
        }
      })
    },
    cancelEdit() {
      this.confirmDiscard().then(ok => {
        if (ok) {
          this.editing = false
          this.draft = this.originalContent
          this.saveError = ''
        }
      })
    },
    saveContent() {
      if (!this.draft || !this.draft.trim()) {
        this.saveError = '配置文件内容不能为空'
        return
      }
      this.saving = true
      this.saveError = ''
      this.$store.dispatch('setting/saveProfileContent', {
        profileId: this.profileId,
        content: this.draft,
        contentHash: this.detail.contentHash
      }).then(data => {
        if (!data) {
          return
        }
        this.rawContent = data.content || this.draft
        this.draft = this.rawContent
        this.originalContent = this.rawContent
        this.detail.contentHash = data.contentHash || ''
        this.detail.active = !!data.active
        this.editing = false
        this.$message.success('配置文件已保存')
        this.$emit('saved', {
          profileId: this.profileId,
          active: !!data.active
        })
      }).catch(error => {
        const message = typeof error === 'string'
          ? error
          : (error && error.message) || '保存配置文件失败'
        this.saveError = message
        this.locateError(message)
      }).finally(() => {
        this.saving = false
      })
    },
    /**
     * 后端返回的 YAML 错误包含行号时, 将光标定位到对应行
     */
    locateError(message) {
      const match = /第(\d+)行/.exec(message || '')
      const editor = this.$refs.editor
      if (!match || !editor || !editor.codemirror) {
        return
      }
      const line = Math.max(Number(match[1]) - 1, 0)
      editor.codemirror.setCursor({ line: line, ch: 0 })
      editor.codemirror.scrollIntoView({ line: line, ch: 0 }, 120)
    },
    confirmDiscard() {
      if (!this.dirty) {
        return Promise.resolve(true)
      }
      return this.$confirm(
        '配置已修改但尚未保存，确认放弃本次修改吗？',
        '放弃修改',
        {
          confirmButtonText: '放弃修改',
          cancelButtonText: '继续编辑',
          type: 'warning'
        }
      ).then(() => true).catch(() => false)
    },
    confirmReset() {
      if (!this.dirty) {
        return Promise.resolve(true)
      }
      return this.$confirm(
        '确认将内容还原为打开时的版本吗？',
        '还原内容',
        {
          confirmButtonText: '还原',
          cancelButtonText: '取消',
          type: 'warning'
        }
      ).then(() => true).catch(() => false)
    },
    handleBeforeClose(done) {
      this.confirmDiscard().then(ok => {
        if (ok) {
          done()
        }
      })
    },
    handleClosed() {
      this.rawContent = ''
      this.draft = ''
      this.originalContent = ''
      this.keyword = ''
      this.editing = false
      this.saveError = ''
      this.detail = {
        writable: false,
        editable: false,
        active: false,
        mainConfig: false,
        contentHash: ''
      }
    },
    emitSwitch() {
      this.$emit('switch', { id: this.profileId, name: this.fileName })
    },
    copyContent() {
      const text = this.editing ? this.draft : this.rawContent
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
      const text = this.editing ? this.draft : this.rawContent
      const blob = new Blob([text], { type: 'text/yaml;charset=utf-8' })
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

.viewer-editing-tip {
  font-size: 12px;
  color: #606266;
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

.viewer-alert {
  margin-bottom: 8px;
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

.code-editor-wrapper {
  flex: 1;
  min-height: 0;
  overflow: hidden;
  border: 1px solid #ebeef5;
  border-radius: 4px;
}

.code-editor {
  height: calc(100vh - 250px);
}

.code-editor >>> .CodeMirror {
  height: 100%;
  font-family: Menlo, Monaco, Consolas, 'Courier New', monospace;
  font-size: 12px;
  line-height: 1.6;
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
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  padding: 12px 16px;
  border-top: 1px solid #ebeef5;
}

.footer-tip {
  margin-right: auto;
  font-size: 12px;
  color: #909399;
}
</style>
