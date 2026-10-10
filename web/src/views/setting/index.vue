<template>
  <div class="setting-page">
    <!-- 左侧菜单 -->
    <aside class="setting-side">
      <div class="side-title">
        <i class="el-icon-setting" />
        <span>系统设置</span>
      </div>
      <el-menu
        :default-active="activeTab"
        class="setting-menu-list"
        @select="handleSelect"
      >
        <el-menu-item v-for="item in menuList" :key="item.key" :index="item.key">
          <i :class="item.icon" />
          <span>{{ item.title }}</span>
        </el-menu-item>
      </el-menu>
    </aside>

    <!-- 右侧内容 -->
    <section class="setting-main">
      <header class="main-header">
        <div class="header-text">
          <h3>{{ currentTitle }}</h3>
          <p>{{ currentDesc }}</p>
        </div>
        <div class="header-actions">
          <el-button
            v-if="activeTab === 'profile'"
            size="mini"
            icon="el-icon-refresh"
            :loading="profileLoading"
            @click="getProfileList"
          >
            刷新
          </el-button>
        </div>
      </header>

      <template v-if="activeTab === 'profile'">
        <!-- 概览 -->
        <div class="stat-row">
          <div class="stat-card">
            <div class="stat-icon stat-icon-blue"><i class="el-icon-star-on" /></div>
            <div class="stat-body">
              <div class="stat-label">当前生效配置</div>
              <div class="stat-value">{{ currentProfileName || '—' }}</div>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon stat-icon-green"><i class="el-icon-files" /></div>
            <div class="stat-body">
              <div class="stat-label">可用配置数</div>
              <div class="stat-value">{{ profileList.length }}</div>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon" :class="switchable ? 'stat-icon-green' : 'stat-icon-orange'">
              <i :class="switchable ? 'el-icon-success' : 'el-icon-warning-outline'" />
            </div>
            <div class="stat-body">
              <div class="stat-label">是否支持切换</div>
              <div class="stat-value">{{ switchable ? '支持' : '不支持' }}</div>
            </div>
          </div>
        </div>

        <el-card shadow="never" class="section-card">
          <div slot="header" class="card-header">
            <span class="card-title">选择配置文件</span>
            <span v-if="!switchable && !profileLoading" class="card-tip">
              <i class="el-icon-info" /> {{ switchDisabledReason }}
            </span>
          </div>

          <div v-loading="profileLoading" class="profile-picker">
            <div
              v-for="item in profileList"
              :key="item.id"
              class="profile-item"
              :class="{
                'is-active': item.active,
                'is-selected': activeProfile === item.id,
                'is-disabled': !switchable
              }"
              @click="selectProfile(item)"
            >
              <div class="pi-top">
                <span class="pi-id">{{ item.id }}</span>
                <el-tag v-if="item.active" size="mini" type="success" effect="dark">使用中</el-tag>
              </div>
              <div class="pi-name" :title="item.name">{{ item.name }}</div>
              <div class="pi-foot">
                <span class="pi-check">
                  <i v-if="activeProfile === item.id" class="el-icon-success" />
                </span>
              </div>
            </div>
            <div v-if="!profileLoading && profileList.length === 0" class="profile-empty">
              未找到配置文件
            </div>
          </div>

          <div v-if="switchable" class="save-bar">
            <span class="save-hint">
              <template v-if="profileChanged">
                已选择「{{ activeProfile }}」，保存后需重启服务生效
              </template>
              <template v-else>当前配置未变更</template>
            </span>
            <div>
              <el-button size="mini" :disabled="!profileChanged" @click="resetSelection">重置</el-button>
              <el-button
                type="primary"
                size="mini"
                icon="el-icon-position"
                :loading="profileSaving"
                :disabled="!profileChanged"
                @click="saveProfile"
              >
                保存
              </el-button>
            </div>
          </div>
        </el-card>

        <el-card shadow="never" class="section-card">
          <div slot="header" class="card-header">
            <span class="card-title">配置文件详情</span>
          </div>
          <el-table
            v-loading="profileLoading"
            :data="profileList"
            size="mini"
            style="width: 100%"
            @current-change="handleCurrentChange"
            ref="profileTable"
          >
            <el-table-column label="状态" width="90" align="center">
              <template v-slot:default="scope">
                <el-tag v-if="scope.row.active" type="success" size="mini">使用中</el-tag>
                <span v-else class="text-muted">—</span>
              </template>
            </el-table-column>
            <el-table-column prop="id" label="配置ID" min-width="140">
              <template v-slot:default="scope">
                <span class="mono">{{ scope.row.id }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="name" label="文件名" min-width="220" show-overflow-tooltip />
            <el-table-column prop="createTime" label="创建时间" width="170">
              <template v-slot:default="scope">
                <span class="text-muted">{{ scope.row.createTime || '—' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="updateTime" label="修改时间" width="170">
              <template v-slot:default="scope">
                <span class="text-muted">{{ scope.row.updateTime || '—' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100" align="center" fixed="right">
              <template v-slot:default="scope">
                <el-button
                  type="text"
                  size="mini"
                  icon="el-icon-view"
                  @click="viewConfig(scope.row)"
                >
                  查看
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </template>

      <el-card v-else shadow="never" class="section-card placeholder-card">
        <i class="placeholder-icon" :class="currentItem.icon" />
        <h4>{{ currentTitle }}</h4>
        <p>{{ currentDesc }}</p>
        <el-tag size="small" type="info">页面内容待接入，敬请期待</el-tag>
      </el-card>
    </section>

    <config-viewer
      ref="viewer"
      :switchable="switchable"
      :is-active="viewerIsActive"
      @switch="handleViewerSwitch"
    />
  </div>
</template>

<script>
import ConfigViewer from './ConfigViewer.vue'

export default {
  name: 'SettingIndex',
  components: { ConfigViewer },
  data() {
    return {
      activeTab: 'profile',
      profileLoading: false,
      profileList: [],
      activeProfile: '',
      savedProfile: '',
      profileSaving: false,
      restarting: false,
      viewerProfileId: '',
      menuList: [
        {
          key: 'webService',
          title: 'WEB服务',
          icon: 'el-icon-monitor',
          desc: 'WEB 服务相关配置项(端口、上下文路径、静态资源目录等)。'
        },
        {
          key: 'sipService',
          title: '国标服务',
          icon: 'el-icon-connection',
          desc: 'GB28181 国标 SIP 服务配置(编号、域、IP、端口、密码等)。'
        },
        {
          key: 'jtService',
          title: '部标服务',
          icon: 'el-icon-truck',
          desc: 'JT/T 1078 部标服务配置(端口、密码等)。'
        },
        {
          key: 'globalPolicy',
          title: '全局策略',
          icon: 'el-icon-set-up',
          desc: '平台全局策略配置(流媒体、录像、订阅、鉴权等)。'
        },
        {
          key: 'profile',
          title: '配置文件',
          icon: 'el-icon-files',
          desc: '查看并切换服务端配置 profile，切换后需重启服务生效。'
        }
      ]
    }
  },
  computed: {
    currentItem() {
      return this.menuList.find(item => item.key === this.activeTab) || {}
    },
    currentTitle() {
      return this.currentItem.title
    },
    currentDesc() {
      return this.currentItem.desc
    },
    currentProfileName() {
      const item = this.profileList.find(row => row.active)
      return item ? item.name : ''
    },
    switchDisabledReason() {
      if (this.profileList.length > 1 && !this.profileList.some(item => item.writable)) {
        return '配置文件位于 jar 内部，无法修改。请使用 --spring.config.location 指定外部配置目录'
      }
      return '当前配置文件未配置 spring.profiles，不支持切换'
    },
    // 后端仅当存在 spring.profiles 且配置可写时返回多个配置, 此时才允许切换
    switchable() {
      return this.profileList.length > 1 && this.profileList.some(item => item.writable)
    },
    profileChanged() {
      return this.switchable && !!this.activeProfile && this.activeProfile !== this.savedProfile
    },
    viewerIsActive() {
      return !!this.viewerProfileId && this.viewerProfileId === this.savedProfile
    }
  },
  created() {
    this.getProfileList()
  },
  methods: {
    handleSelect(key) {
      this.activeTab = key
    },
    selectProfile(item) {
      if (!this.switchable) {
        return
      }
      this.activeProfile = item.id
      if (this.$refs.profileTable) {
        this.$refs.profileTable.setCurrentRow(item)
      }
    },
    handleCurrentChange(row) {
      if (row) {
        this.activeProfile = row.id
      }
    },
    resetSelection() {
      this.activeProfile = this.savedProfile
      const activeItem = this.profileList.find(item => item.active)
      if (activeItem && this.$refs.profileTable) {
        this.$refs.profileTable.setCurrentRow(activeItem)
      }
    },
    viewConfig(row) {
      this.viewerProfileId = row.id
      this.$refs.viewer.open(row)
    },
    handleViewerSwitch(payload) {
      this.activeProfile = payload.id
      const item = this.profileList.find(row => row.id === payload.id)
      if (item && this.$refs.profileTable) {
        this.$refs.profileTable.setCurrentRow(item)
      }
      this.$refs.viewer.visible = false
      this.saveProfile()
    },
    saveProfile() {
      if (!this.profileChanged) {
        return
      }
      const target = this.activeProfile
      this.$confirm(
        `确认将配置文件切换为「${target}」吗？切换后需要重启服务才能生效。`,
        '切换配置文件',
        {
          confirmButtonText: '保存并重启',
          cancelButtonText: '仅保存',
          distinguishCancelAndClose: true,
          type: 'warning'
        }
      ).then(() => {
        this.doSaveProfile(target, true)
      }).catch(action => {
        if (action === 'cancel') {
          this.doSaveProfile(target, false)
        }
      })
    },
    doSaveProfile(target, needRestart) {
      this.profileSaving = true
      this.$store.dispatch('setting/switchProfile', target)
        .then(() => {
          this.savedProfile = target
          this.$message.success('配置文件已保存')
          this.getProfileList()
          if (needRestart) {
            this.confirmRestart()
          } else {
            this.$message.info('配置将在下次服务重启后生效')
          }
        })
        .catch(error => {
          this.$message.error(typeof error === 'string' ? error : '保存配置文件失败')
        })
        .finally(() => {
          this.profileSaving = false
        })
    },
    confirmRestart() {
      this.$confirm(
        '服务即将重启，重启期间页面将无法访问，请稍后刷新页面。',
        '重启服务',
        {
          confirmButtonText: '立即重启',
          cancelButtonText: '稍后手动重启',
          type: 'warning'
        }
      ).then(() => {
        this.doRestart()
      }).catch(() => {
        this.$message.info('可稍后手动重启服务使配置生效')
      })
    },
    doRestart() {
      this.$store.dispatch('setting/restartServer')
        .then(() => {
          this.$message.success('服务正在重启，请稍后刷新页面')
          this.restarting = true
          this.waitForRestart()
        })
        .catch(error => {
          this.$message.error(typeof error === 'string' ? error : '触发重启失败')
        })
    },
    // 服务重启后轮询等待恢复
    waitForRestart() {
      let retry = 0
      const maxRetry = 60
      const timer = setInterval(() => {
        retry += 1
        this.$store.dispatch('setting/getProfileList')
          .then(data => {
            clearInterval(timer)
            this.profileList = data || []
            this.restarting = false
            this.$message.success('服务已恢复')
          })
          .catch(() => {
            if (retry >= maxRetry) {
              clearInterval(timer)
              this.restarting = false
              this.$message.warning('等待服务恢复超时，请手动刷新页面')
            }
          })
      }, 3000)
    },
    getProfileList() {
      this.profileLoading = true
      this.$store.dispatch('setting/getProfileList')
        .then(data => {
          this.profileList = data || []
          // 默认选中当前正在使用的配置文件
          const activeItem = this.profileList.find(item => item.active)
          const selected = activeItem || this.profileList[0]
          if (selected) {
            this.activeProfile = selected.id
            this.savedProfile = activeItem ? activeItem.id : ''
          }
          this.$nextTick(() => {
            if (activeItem && this.$refs.profileTable) {
              this.$refs.profileTable.setCurrentRow(activeItem)
            }
          })
        })
        .catch(error => {
          console.log(error)
        })
        .finally(() => {
          this.profileLoading = false
        })
    }
  }
}
</script>

<style scoped>
.setting-page {
  display: flex;
  height: calc(100vh - 124px);
  background: #f5f7fa;
}

/* ---------- 左侧菜单 ---------- */
.setting-side {
  width: 190px;
  min-width: 190px;
  height: 100%;
  background: #fff;
  border-right: 1px solid #e8eaef;
  overflow: auto;
}

.side-title {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 48px;
  padding: 0 16px;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  border-bottom: 1px solid #f0f2f5;
}

.setting-menu-list {
  border-right: none;
  padding-top: 6px;
}

.setting-menu-list >>> .el-menu-item {
  height: 42px;
  line-height: 42px;
  margin: 2px 8px;
  border-radius: 6px;
  color: #5a6474;
  font-size: 13px;
}

.setting-menu-list >>> .el-menu-item i {
  margin-right: 8px;
  color: inherit;
}

.setting-menu-list >>> .el-menu-item:hover {
  background: #f2f6fc;
}

.setting-menu-list >>> .el-menu-item.is-active {
  background: #ecf5ff;
  color: #409eff;
  font-weight: 600;
}

/* ---------- 右侧内容 ---------- */
.setting-main {
  flex: 1;
  min-width: 0;
  overflow: auto;
  padding: 16px 20px 24px;
}

.main-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 16px;
}

.header-text h3 {
  margin: 0 0 4px;
  font-size: 17px;
  font-weight: 600;
  color: #1f2d3d;
}

.header-text p {
  margin: 0;
  font-size: 12px;
  color: #8b95a5;
}

/* ---------- 概览卡片 ---------- */
.stat-row {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.stat-card {
  flex: 1;
  min-width: 180px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  background: #fff;
  border: 1px solid #e8eaef;
  border-radius: 8px;
  transition: box-shadow 0.2s;
}

.stat-card:hover {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.stat-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  border-radius: 8px;
  font-size: 18px;
  flex: 0 0 38px;
}

.stat-icon-blue {
  background: #ecf5ff;
  color: #409eff;
}

.stat-icon-green {
  background: #f0f9eb;
  color: #67c23a;
}

.stat-icon-orange {
  background: #fdf6ec;
  color: #e6a23c;
}

.stat-body {
  min-width: 0;
}

.stat-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 2px;
}

.stat-value {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ---------- 卡片 ---------- */
.section-card {
  border: 1px solid #e8eaef;
  border-radius: 8px;
  margin-bottom: 16px;
}

.section-card >>> .el-card__header {
  padding: 12px 16px;
  border-bottom: 1px solid #f0f2f5;
}

.card-header {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.card-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.card-tip {
  font-size: 12px;
  color: #e6a23c;
}

/* ---------- 配置选择 ---------- */
.profile-picker {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  min-height: 78px;
}

.profile-item {
  position: relative;
  width: 190px;
  padding: 10px 12px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  background: #fff;
  cursor: pointer;
  transition: all 0.2s;
  box-sizing: border-box;
}

.profile-item:hover {
  border-color: #b3d8ff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.12);
}

.profile-item.is-selected {
  border-color: #409eff;
  background: #f5faff;
}

.profile-item.is-active {
  border-color: #67c23a;
}

.profile-item.is-disabled {
  cursor: not-allowed;
  opacity: 0.75;
}

.profile-item.is-disabled:hover {
  border-color: #e4e7ed;
  box-shadow: none;
}

.pi-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
  margin-bottom: 6px;
}

.pi-id {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pi-name {
  font-size: 12px;
  color: #909399;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pi-foot {
  height: 16px;
  text-align: right;
}

.pi-check {
  font-size: 13px;
  color: #409eff;
}

.profile-empty {
  padding: 20px;
  color: #909399;
  font-size: 13px;
}

/* ---------- 保存条 ---------- */
.save-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px dashed #ebeef5;
}

.save-hint {
  font-size: 12px;
  color: #909399;
}

/* ---------- 表格 ---------- */
.mono {
  font-family: Menlo, Monaco, Consolas, monospace;
  color: #1f6feb;
}

.text-muted {
  color: #a8b0bd;
}

/* ---------- 占位页 ---------- */
.placeholder-card >>> .el-card__body {
  padding: 60px 20px;
}

.placeholder-card {
  text-align: center;
}

.placeholder-icon {
  font-size: 44px;
  color: #c8d0dc;
}

.placeholder-card h4 {
  margin: 16px 0 8px;
  font-size: 16px;
  color: #303133;
}

.placeholder-card p {
  margin: 0 0 16px;
  font-size: 13px;
  color: #8b95a5;
}
</style>
