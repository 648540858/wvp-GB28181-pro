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
      <!-- 顶部：配置文件操作栏 -->
      <div class="profile-bar">
        <div class="profile-bar-head">
          <div class="pb-current">
            <i class="el-icon-collection-tag" />
            <span class="pb-label">当前配置</span>
            <span class="pb-name" :title="currentProfileName">{{ currentProfileName || '—' }}</span>
          </div>
          <div class="pb-actions">
            <el-button size="mini" icon="el-icon-view" :disabled="!currentProfileName" @click="viewCurrentConfig">查看</el-button>
            <el-button size="mini" icon="el-icon-refresh" :loading="profileLoading" @click="getProfileList">刷新</el-button>
            <el-button
              size="mini"
              :icon="profileExpanded ? 'el-icon-arrow-up' : 'el-icon-arrow-down'"
              @click="profileExpanded = !profileExpanded"
            >
              {{ profileExpanded ? '收起' : '展开' }}
            </el-button>
          </div>
        </div>

        <div v-show="profileExpanded" v-loading="profileLoading" class="profile-panel">
          <div v-if="!switchable && !profileLoading" class="pb-tip">
            <i class="el-icon-info" /> {{ switchDisabledReason }}
          </div>
          <div class="profile-grid">
            <div
              v-for="item in profileList"
              :key="item.id"
              class="profile-card"
              :class="{ 'is-active': item.active }"
            >
              <div class="pc-top">
                <span class="pc-id">{{ item.id }}</span>
                <el-tag v-if="item.active" size="mini" type="success" effect="dark">使用中</el-tag>
              </div>
              <div class="pc-name" :title="item.name">{{ item.name }}</div>
              <div class="pc-actions">
                <el-button type="text" size="mini" icon="el-icon-view" @click="viewConfig(item)">详情</el-button>
                <el-button
                  v-if="switchable && !item.active"
                  type="text"
                  size="mini"
                  icon="el-icon-position"
                  :loading="profileSaving && switchTarget === item.id"
                  @click="switchToProfile(item)"
                >切换</el-button>
              </div>
            </div>
            <div v-if="!profileLoading && profileList.length === 0" class="profile-empty">未找到配置文件</div>
          </div>
        </div>
      </div>

      <header class="main-header">
        <div class="header-text">
          <h3>{{ currentTitle }}</h3>
          <p>{{ currentDesc }}</p>
        </div>
        <div class="header-actions">
          <el-button
            v-if="activeTab === 'webService'"
            size="mini"
            icon="el-icon-refresh"
            @click="refreshWebServer"
          >
            刷新
          </el-button>
          <el-button
            v-if="activeTab === 'sipService'"
            size="mini"
            icon="el-icon-refresh"
            @click="refreshSipServer"
          >
            刷新
          </el-button>
          <el-button
            v-if="activeTab === 'jtService'"
            size="mini"
            icon="el-icon-refresh"
            @click="refreshJtServer"
          >
            刷新
          </el-button>
          <el-button
            v-if="activeTab === 'globalPolicy'"
            size="mini"
            icon="el-icon-refresh"
            @click="refreshGlobalPolicy"
          >
            刷新
          </el-button>
        </div>
      </header>

      <web-server-config v-if="activeTab === 'webService'" ref="webServer" />
      <sip-server-config v-else-if="activeTab === 'sipService'" ref="sipServer" />
      <jt-server-config v-else-if="activeTab === 'jtService'" ref="jtServer" />
      <global-policy-config v-else-if="activeTab === 'globalPolicy'" ref="globalPolicy" />

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
import WebServerConfig from './WebServerConfig.vue'
import SipServerConfig from './SipServerConfig.vue'
import JtServerConfig from './JtServerConfig.vue'
import GlobalPolicyConfig from './GlobalPolicyConfig.vue'

export default {
  name: 'SettingIndex',
  components: { ConfigViewer, WebServerConfig, SipServerConfig, JtServerConfig, GlobalPolicyConfig },
  data() {
    return {
      activeTab: 'webService',
      profileLoading: false,
      profileList: [],
      savedProfile: '',
      profileSaving: false,
      profileExpanded: false,
      switchTarget: '',
      restarting: false,
      viewerProfileId: '',
      menuList: [
        {
          key: 'webService',
          title: 'WEB服务',
          icon: 'el-icon-monitor',
          desc: 'WEB 服务相关配置项(端口、HTTPS 证书等)。'
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
          desc: 'JT/T 808+1078 部标服务配置(端口、密码等)。'
        },
        {
          key: 'globalPolicy',
          title: '全局策略',
          icon: 'el-icon-set-up',
          desc: '平台全局策略配置(订阅、鉴权等)。'
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
    refreshWebServer() {
      if (this.$refs.webServer) {
        this.$refs.webServer.reload()
      }
    },
    refreshSipServer() {
      if (this.$refs.sipServer) {
        this.$refs.sipServer.reload()
      }
    },
    refreshJtServer() {
      if (this.$refs.jtServer) {
        this.$refs.jtServer.reload()
      }
    },
    refreshGlobalPolicy() {
      if (this.$refs.globalPolicy) {
        this.$refs.globalPolicy.reload()
      }
    },
    viewConfig(row) {
      this.viewerProfileId = row.id
      this.$refs.viewer.open(row)
    },
    handleViewerSwitch(payload) {
      this.$refs.viewer.visible = false
      const item = this.profileList.find(row => row.id === payload.id)
      if (item && !item.active) {
        this.switchToProfile(item)
      }
    },
    switchToProfile(item) {
      if (!this.switchable || item.active) {
        return
      }
      const target = item.id
      this.switchTarget = target
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
      }).finally(() => {
        this.switchTarget = ''
      })
    },
    viewCurrentConfig() {
      const item = this.profileList.find(row => row.active)
      if (item) {
        this.viewConfig(item)
      }
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
          const activeItem = this.profileList.find(item => item.active)
          this.savedProfile = activeItem ? activeItem.id : ''
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

/* ---------- 顶部配置文件操作栏 ---------- */
.profile-bar {
  position: sticky;
  top: 0;
  z-index: 10;
  margin-bottom: 16px;
  background: #fff;
  border: 1px solid #e8eaef;
  border-radius: 8px;
}

.profile-bar-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 16px;
}

.pb-current {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  font-size: 13px;
  color: #5a6474;
}

.pb-current > i {
  color: #409eff;
  font-size: 16px;
}

.pb-label {
  color: #909399;
}

.pb-name {
  font-weight: 600;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pb-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 0 0 auto;
}

.profile-panel {
  border-top: 1px solid #f0f2f5;
  padding: 12px 16px 16px;
}

.pb-tip {
  margin-bottom: 10px;
  font-size: 12px;
  color: #e6a23c;
}

.profile-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  min-height: 78px;
}

.profile-card {
  width: 200px;
  padding: 10px 12px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  background: #fff;
  box-sizing: border-box;
  transition: all 0.2s;
}

.profile-card:hover {
  border-color: #b3d8ff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.12);
}

.profile-card.is-active {
  border-color: #67c23a;
  background: #f6ffed;
}

.pc-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
  margin-bottom: 6px;
}

.pc-id {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pc-name {
  font-size: 12px;
  color: #909399;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  min-height: 16px;
}

.pc-actions {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 8px;
}

.profile-empty {
  padding: 20px;
  color: #909399;
  font-size: 13px;
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
