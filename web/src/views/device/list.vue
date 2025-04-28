<template>
  <div id="app" style="height: calc(100vh - 124px);">
    <el-form :inline="true" size="mini">
      <el-form-item label="搜索">
        <el-input
          v-model="searchSrt"
          style="margin-right: 1rem; width: auto;"
          placeholder="关键字"
          prefix-icon="el-icon-search"
          clearable
          @input="initData"
        />
      </el-form-item>
      <el-form-item label="在线状态">
        <el-select
          v-model="online"
          style="width: 8rem; margin-right: 1rem;"
          placeholder="请选择"
          default-first-option
          @change="initData"
        >
          <el-option label="全部" value="" />
          <el-option label="在线" value="true" />
          <el-option label="离线" value="false" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button icon="el-icon-plus" style="margin-right: 1rem;" type="primary" @click="add">添加设备</el-button>
        <el-button icon="el-icon-info" style="margin-right: 1rem;" @click="showInfo()">接入信息
        </el-button>
      </el-form-item>
      <el-form-item style="float: right;">
        <el-button
          icon="el-icon-refresh-right"
          circle
          :loading="getDeviceListLoading"
          @click="getDeviceList()"
        />
      </el-form-item>
    </el-form>
    <!--设备列表-->
    <el-table
      size="small"
      :data="deviceList"
      height="calc(100% - 64px)"
      header-row-class-name="table-header"
    >
      <el-table-column prop="name" label="名称" min-width="160" />
      <el-table-column prop="deviceId" label="设备编号" min-width="160" />
      <el-table-column label="地址" min-width="180">
        <template v-slot:default="scope">
          <div slot="reference" class="name-wrapper">
            <el-tag v-if="scope.row.hostAddress" size="medium">{{ scope.row.transport.toLowerCase() }}://{{ scope.row.hostAddress }}</el-tag>
            <el-tag v-if="!scope.row.hostAddress" size="medium">未知</el-tag>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="manufacturer" label="厂家" min-width="100" />
      <el-table-column label="流传输模式" min-width="160">
        <template v-slot:default="scope">
          <el-select
            v-model="scope.row.streamMode"
            size="mini"
            placeholder="请选择"
            style="width: 120px"
            @change="transportChange(scope.row)"
          >
            <el-option key="UDP" label="UDP" value="UDP" />
            <el-option key="TCP-ACTIVE" label="TCP主动模式" value="TCP-ACTIVE" />
            <el-option key="TCP-PASSIVE" label="TCP被动模式" value="TCP-PASSIVE" />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column label="通道数" min-width="80">
        <template v-slot:default="scope">
          <span style="font-size: 1rem">{{ scope.row.channelCount }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" min-width="80">
        <template v-slot:default="scope">
          <div slot="reference" class="name-wrapper">
            <el-tag
              v-if="scope.row.onLine && myServerId !== scope.row.serverId"
              size="medium"
              style="border-color: #ecf1af"
            >在线
            </el-tag>
            <el-tag v-if="scope.row.onLine && myServerId === scope.row.serverId" size="medium">在线
            </el-tag>
            <el-tag v-if="!scope.row.onLine" size="medium" type="info">离线</el-tag>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="订阅" min-width="160">
        <template v-slot:default="scope">
          <el-checkbox
            label="目录"
            :checked="scope.row.subscribeCycleForCatalog > 0"
            @change="(e)=>subscribeForCatalog(scope.row.id, e)"
          />
          <el-checkbox
            label="位置"
            :checked="scope.row.subscribeCycleForMobilePosition > 0"
            @change="(e)=>subscribeForMobilePosition(scope.row.id, e)"
          />
          <!--          <el-checkbox label="报警" disabled :checked="scope.row.subscribeCycleForAlarm > 0"></el-checkbox>-->
        </template>
      </el-table-column>
      <el-table-column prop="keepaliveTime" label="最近心跳" min-width="140" />
      <el-table-column prop="registerTime" label="最近注册" min-width="140" />
      <el-table-column label="操作" min-width="300" fixed="right">
        <template v-slot:default="scope">
          <el-button
            type="text"
            size="medium"
            :disabled="scope.row.online===0"
            icon="el-icon-refresh"
            @click="refDevice(scope.row)"
            @mouseover="getTooltipContent(scope.row.deviceId)"
          >刷新
          </el-button>
          <el-divider direction="vertical" />
          <el-button
            type="text"
            size="medium"
            icon="el-icon-video-camera"
            @click="showChannelList(scope.row)"
          >通道
          </el-button>
          <el-divider direction="vertical" />
          <el-button size="medium" icon="el-icon-edit" type="text" @click="edit(scope.row)">编辑</el-button>
          <el-divider direction="vertical" />
          <el-dropdown @command="(command)=>{moreClick(command, scope.row)}">
            <el-button size="medium" type="text">
              操作<i class="el-icon-arrow-down el-icon--right" />
            </el-button>
            <el-dropdown-menu>
              <el-dropdown-item command="delete" style="color: #f56c6c">
                删除
              </el-dropdown-item>
              <el-dropdown-item command="setGuard" :disabled="!scope.row.onLine">
                布防
              </el-dropdown-item>
              <el-dropdown-item command="resetGuard" :disabled="!scope.row.onLine">
                撤防
              </el-dropdown-item>
              <el-dropdown-item command="syncBasicParam" :disabled="!scope.row.onLine">
                基础配置同步
              </el-dropdown-item>
            </el-dropdown-menu>
          </el-dropdown>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      style="text-align: right"
      :current-page="currentPage"
      :page-size="count"
      :page-sizes="[15, 25, 35, 50]"
      layout="total, sizes, prev, pager, next"
      :total="total"
      @size-change="handleSizeChange"
      @current-change="currentChange"
    />
    <deviceEdit ref="deviceEdit" />
    <syncChannelProgress ref="syncChannelProgress" />
    <configInfo ref="configInfo" />
  </div>
</template>

<script>
import deviceEdit from '../dialog/deviceEdit.vue'
import syncChannelProgress from '../dialog/SyncChannelProgress.vue'
import configInfo from '../dialog/configInfo.vue'
import Vue from 'vue'

export default {
  name: 'App',
  components: {
    configInfo,
    deviceEdit,
    syncChannelProgress
  },
  data() {
    return {
      deviceList: [], // 设备列表
      currentDevice: {}, // 当前操作设备对象
      searchSrt: '',
      online: null,
      videoComponentList: [],
      updateLooper: 0, // 数据刷新轮训标志
      currentDeviceChannelsLength: 0,
      currentPage: 1,
      count: 15,
      total: 0,
      getDeviceListLoading: false
    }
  },
  computed: {
    Vue() {
      return Vue
    },
    myServerId() {
      return this.$store.getters.serverId
    }
  },
  mounted() {
    this.initData()
    this.updateLooper = setInterval(this.getDeviceList, 10000)
  },
  destroyed() {
    this.$destroy('videojs')
    clearTimeout(this.updateLooper)
  },
  methods: {
    initData: function() {
      this.currentPage = 1
      this.total = 0
      this.getDeviceList()
    },
    currentChange: function(val) {
      this.currentPage = val
      this.getDeviceList()
    },
    handleSizeChange: function(val) {
      this.count = val
      this.getDeviceList()
    },
    getDeviceList: function() {
      this.getDeviceListLoading = true
      this.$store.dispatch('device/queryDevices', {
        page: this.currentPage,
        count: this.count,
        query: this.searchSrt,
        status: this.online
      }).then((data) => {
        this.total = data.total
        this.deviceList = data.list
      }).finally(() => {
        this.getDeviceListLoading = false
      })
    },
    deleteDevice: function(row) {
      let msg = '确定删除此设备？'
      if (row.online !== 0) {
        msg = '在线设备删除后仍可通过注册再次上线。<br/>如需彻底删除请先将设备离线。<br/><strong>确定删除此设备？</strong>'
      }
      this.$confirm(msg, '提示', {
        dangerouslyUseHTMLString: true,
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        center: true,
        type: 'warning'
      }).then(() => {
        this.$store.dispatch('device/deleteDevice', row.deviceId)
          .then((data) => {
            this.getDeviceList()
          })
      })
    },
    showChannelList: function(row) {
      this.$emit('show-channel', row.deviceId)
      // this.$router.push(`/device/?deviceId=${row.deviceId}`)
    },
    showDevicePosition: function(row) {
      this.$router.push(`/map?deviceId=${row.deviceId}`)
    },

    // gb28181平台对接
    // 刷新设备信息
    refDevice: function(itemData) {
      console.log('刷新对应设备:' + itemData.deviceId)
      this.$store.dispatch('device/sync', itemData.deviceId)
        .then(data => {
          if (data && data.errorMsg) {
            this.$message({
              showClose: true,
              message: data.errorMsg,
              type: 'error'
            })
            return
          }

          this.$refs.syncChannelProgress.openDialog(itemData.deviceId, () => {
            this.getDeviceList()
          })
        }).finally(() => {
          this.getDeviceList()
        })
    },

    getTooltipContent: async function(deviceId) {
      let result = ''
      await this.$store.dispatch('device/queryDeviceSyncStatus', deviceId)
        .then((data) => {
          if (data.errorMsg !== null) {
            result = data.errorMsg
          }
          result = `同步中...[${data.current}/${data.total}]`
        }).catch(error => {
          result = error
        })
      return result
    },
    transportChange: function(row) {
      console.log(`修改传输方式为 ${row.streamMode}：${row.deviceId} `)
      console.log(row.streamMode)
      this.$store.dispatch('device/updateDeviceTransport', [row.deviceId, row.streamMode])
    },
    edit: function(row) {
      this.$refs.deviceEdit.openDialog(row, () => {
        this.$refs.deviceEdit.close()
        this.$message({
          showClose: true,
          message: '设备修改成功，通道字符集将在下次更新生效',
          type: 'success'
        })
        setTimeout(this.getDeviceList, 200)
      })
    },
    add: function() {
      this.$refs.deviceEdit.openDialog(null, () => {
        this.$refs.deviceEdit.close()
        this.$message({
          showClose: true,
          message: '添加成功',
          type: 'success'
        })
        setTimeout(this.getDeviceList, 200)
      })
    },
    showInfo: function() {
      this.$store.dispatch('server/getSystemConfig')
        .then((data) => {
          this.serverId = data.addOn.serverId
          this.$refs.configInfo.openDialog(data)
        })
    },
    moreClick: function(command, itemData) {
      if (command === 'setGuard') {
        this.setGuard(itemData)
      } else if (command === 'resetGuard') {
        this.resetGuard(itemData)
      } else if (command === 'delete') {
        this.deleteDevice(itemData)
      } else if (command === 'syncBasicParam') {
        this.syncBasicParam(itemData)
      }
    },
    setGuard: function(itemData) {
      this.$store.dispatch('device/setGuard', itemData.deviceId)
        .then((data) => {
          this.$message.success({
            showClose: true,
            message: '布防成功'
          })
        }).catch((error) => {
          this.$message.error({
            showClose: true,
            message: error.message
          })
        })
    },
    resetGuard: function(itemData) {
      this.$store.dispatch('device/ResetGuard', itemData.deviceId)
        .then((data) => {
          this.$message.success({
            showClose: true,
            message: '撤防成功'
          })
        }).catch((error) => {
          this.$message.error({
            showClose: true,
            message: error.message
          })
        })
    },
    subscribeForCatalog: function(data, value) {
      this.$store.dispatch('device/subscribeCatalog', {
        id: data,
        cycle: value ? 60 : 0
      }).then((data) => {
        this.$message.success({
          showClose: true,
          message: value ? '订阅成功' : '取消订阅成功'
        })
      }).catch((error) => {
        this.$message.error({
          showClose: true,
          message: error.message
        })
      })
    },
    subscribeForMobilePosition: function(data, value) {
      this.$store.dispatch('device/subscribeMobilePosition', {
        id: data,
        cycle: value ? 60 : 0,
        interval: value ? 5 : 0
      }).then((data) => {
        this.$message.success({
          showClose: true,
          message: value ? '订阅成功' : '取消订阅成功'
        })
      }).catch((error) => {
        this.$message.error({
          showClose: true,
          message: error.message
        })
      })
    },
    syncBasicParam: function(data) {
      this.$store.dispatch('device/queryBasicParam')
        .then((data) => {
          this.$message.success({
            showClose: true,
            message: `配置已同步，当前心跳间隔： ${data.BasicParam.HeartBeatInterval} 心跳间隔:${res.data.data.BasicParam.HeartBeatCount}`
          })
        }).catch((error) => {
          this.$message.error({
            showClose: true,
            message: error.message
          })
        })
    }
  }
}
</script>
