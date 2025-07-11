<template>
  <div id="channelList" style="height: calc(100vh - 124px);">
    <div v-if="!jtChannel">
      <el-form :inline="true" size="mini">
        <el-form-item style="margin-right: 2rem">
          <el-page-header content="通道列表" @back="showDevice" />
        </el-form-item>
        <el-form-item label="搜索">
          <el-input
            v-model="searchSrt"
            style="margin-right: 1rem; width: auto;"
            placeholder="关键字"
            prefix-icon="el-icon-search"
            clearable
            @input="search"
          />
        </el-form-item>
        <el-form-item>
          <el-button icon="el-icon-plus" size="mini" style="margin-right: 1rem;" type="primary" @click="add">添加通道</el-button>
        </el-form-item>
        <el-form-item style="float: right;">
          <el-button icon="el-icon-refresh-right" circle @click="refresh()" />
        </el-form-item>
      </el-form>
      <el-container v-loading="isLoging" style="height: 82vh;">
        <el-main style="padding: 5px;">
          <el-table
            ref="channelListTable"
            :data="deviceChannelList"
            :height="winHeight"
            style="width: 100%"
            header-row-class-name="table-header"
          >
            <el-table-column prop="channelId" label="通道编号" min-width="180" />
            <el-table-column prop="name" label="名称" min-width="180" />
            <el-table-column label="快照" min-width="100">
              <template v-slot:default="scope">
                <el-image
                  :src="getSnap(scope.row)"
                  :preview-src-list="getBigSnap(scope.row)"
                  :fit="'contain'"
                  style="width: 60px"
                  @error="getSnapErrorEvent(scope.row.deviceId, scope.row.channelId)"
                >
                  <div slot="error" class="image-slot">
                    <i class="el-icon-picture-outline" />
                  </div>
                </el-image>
              </template>
            </el-table-column>
            <el-table-column label="开启音频" min-width="100">
              <template slot-scope="scope">
                <el-switch v-model="scope.row.hasAudio" active-color="#409EFF" @change="updateChannel(scope.row)" />
              </template>
            </el-table-column>
            <el-table-column label="操作" min-width="340" fixed="right">
              <template slot-scope="scope">
                <el-button
                  size="medium"
                  :disabled="device == null || device.online === 0"
                  icon="el-icon-video-play"
                  type="text"
                  @click="sendDevicePush(scope.row)"
                >播放
                </el-button>
                <el-button
                  v-if="!!scope.row.stream"
                  size="medium"
                  :disabled="device == null || device.online === 0"
                  icon="el-icon-switch-button"
                  type="text"
                  style="color: #f56c6c"
                  @click="stopDevicePush(scope.row)"
                >停止
                </el-button>
                <el-divider direction="vertical" />
                <el-button
                  size="medium"
                  type="text"
                  icon="el-icon-edit"
                  @click="handleEdit(scope.row)"
                >
                  编辑
                </el-button>
                <el-divider direction="vertical" />
                <el-dropdown @command="(command)=>{moreClick(command, scope.row)}">
                  <el-button size="medium" type="text">
                    更多功能<i class="el-icon-arrow-down el-icon--right" />
                  </el-button>
                  <el-dropdown-menu slot="dropdown">
                    <el-dropdown-item command="records" :disabled="device == null || device.online === 0">
                      设备录像</el-dropdown-item>
                    <el-dropdown-item command="cloudRecords" :disabled="device == null || device.online === 0">
                      云端录像</el-dropdown-item>
                    <el-dropdown-item command="shooting" v-bind:disabled="device == null || device.online === 0" >
                      抓图</el-dropdown-item>
                    <el-dropdown-item command="searchData" v-bind:disabled="device == null || device.online === 0" >
                      数据检索</el-dropdown-item>
                  </el-dropdown-menu>
                </el-dropdown>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            style="float: right"
            :current-page="currentPage"
            :page-size="count"
            :page-sizes="[15, 25, 35, 50]"
            layout="total, sizes, prev, pager, next"
            :total="total"
            @size-change="handleSizeChange"
            @current-change="currentChange"
          />
        </el-main>
      </el-container>
    </div>
    <devicePlayer ref="devicePlayer" />
    <channelEdit v-if="jtChannel" ref="channelEdit" :jt-channel="jtChannel" :close-edit="closeEdit" />
  </div>
</template>

<script>
import devicePlayer from '../jtDevicePlayer.vue'
import channelEdit from './edit.vue'
import dayjs from 'dayjs'

export default {
  name: 'ChannelList',
  components: {
    channelEdit,
    devicePlayer
  },
  props: {
    deviceId: {
      type: String,
      default: null
    }
  },
  data() {
    return {
      device: null,
      deviceChannelList: [],
      updateLooper: 0, // 数据刷新轮训标志
      searchSrt: '',
      channelType: '',
      online: '',
      winHeight: window.innerHeight - 200,
      currentPage: 1,
      count: 15,
      total: 0,
      beforeUrl: '/jtDeviceList',
      isLoging: false,
      loadSnap: {},
      jtChannel: null
    }
  },

  mounted() {
    this.initParam()
    this.initData()
  },
  destroyed() {
    this.$destroy('videojs')
    clearTimeout(this.updateLooper)
  },
  methods: {
    initData: function() {
      this.getDeviceChannelList()
    },
    initParam: function() {
      this.currentPage = 1
      this.count = 15
      this.$store.dispatch('jtDevice/queryDeviceById', this.deviceId)
        .then(data => {
          this.device = data
        })
        .catch(err => {
          console.error(err)
        })
    },
    currentChange: function(val) {
      this.currentPage = val
      this.initData()
    },
    handleSizeChange: function(val) {
      this.count = val
      this.getDeviceChannelList()
    },
    getDeviceChannelList: function() {
      if (typeof (this.deviceId) === 'undefined') return
      this.$store.dispatch('jtDevice/queryChannels', {
        page: this.currentPage,
        count: this.count,
        query: this.searchSrt,
        deviceId: this.deviceId
      })
        .then(data => {
          this.total = data.total
          this.deviceChannelList = data.list
          // 防止出现表格错位
          this.$nextTick(() => {
            this.$refs.channelListTable.doLayout()
          })
        })
    },

    // 通知设备上传媒体流
    sendDevicePush: function(itemData) {
      this.isLoging = true
      const channelId = itemData.channelId
      console.log('通知设备推流1：' + this.device.phoneNumber + ' : ' + channelId)

      this.$store.dispatch('jtDevice/play', {
        phoneNumber: this.device.phoneNumber,
        channelId: channelId,
        type: 0
      })
        .then(data => {
          setTimeout(() => {
            const snapId = this.device.phoneNumber + '_' + channelId
            this.loadSnap[this.device.phoneNumber + channelId] = 0
            this.getSnapErrorEvent(snapId)
          }, 5000)
          itemData.streamId = data.stream
          this.$refs.devicePlayer.openDialog('media', this.device.phoneNumber, channelId, {
            streamInfo: data,
            hasAudio: itemData.hasAudio
          })
          setTimeout(() => {
            this.initData()
          }, 1000)
        })
        .catch(err => {
          console.error(err)
        })
        .finally(() => {
          this.isLoging = false
        })
    },
    moreClick: function(command, itemData) {
      if (command === 'records') {
        this.queryRecords(itemData)
      } else if (command === 'cloudRecords') {
        this.queryCloudRecords(itemData)
      } else if (command === 'shooting') {
        this.shooting(itemData)
      } else {
        this.$message.info('尚不支持')
      }
    },
    queryRecords: function(itemData) {
      this.$router.push(`/jtDevice/record/${this.device.phoneNumber}/${itemData.channelId}`)
    },
    queryCloudRecords: function(itemData) {
      const deviceId = this.device.phoneNumber
      const channelId = itemData.channelId
      this.$router.push(`/cloudRecord/detail/rtp/jt_${deviceId}_${channelId}`)
    },
    stopDevicePush: function(itemData) {
      this.$store.dispatch('jtDevice/stopPlay', {
        phoneNumber: this.device.phoneNumber,
        channelId: itemData.channelId
      })
        .then((data) => {
          this.initData()
        })
        .catch(function(error) {
          console.error(error)
        })
    },
    getSnap: function(row) {
      const baseUrl = window.baseUrl ? window.baseUrl : ''
      return ((process.env.NODE_ENV === 'development') ? process.env.VUE_APP_BASE_API : baseUrl) + '/api/device/query/snap/' + this.device.phoneNumber + '/' + row.channelId
    },
    getBigSnap: function(row) {
      return [this.getSnap(row)]
    },
    getSnapErrorEvent: function(deviceId, channelId) {
      if (typeof (this.loadSnap[deviceId + channelId]) !== 'undefined') {
        console.log('下载截图' + this.loadSnap[deviceId + channelId])
        if (this.loadSnap[deviceId + channelId] > 5) {
          delete this.loadSnap[deviceId + channelId]
          return
        }
        setTimeout(() => {
          const baseUrl = window.baseUrl ? window.baseUrl : ''
          const url = (process.env.NODE_ENV === 'development' ? process.env.VUE_APP_BASE_API : baseUrl) + '/api/device/query/snap/' + deviceId + '/' + channelId
          this.loadSnap[deviceId + channelId]++
          document.getElementById(deviceId + channelId).setAttribute('src', url + '?' + new Date().getTime())
        }, 1000)
      }
    },
    showDevice: function() {
      this.$emit('show-device')
    },
    search: function() {
      this.currentPage = 1
      this.total = 0
      this.initData()
    },
    updateChannel: function(row) {
      this.$store.dispatch('jtDevice/updateChannel', row)
        .catch((e) => {
          console.log(e)
        })
    },
    refresh: function() {
      this.initData()
    },
    add: function() {
      this.jtChannel = {
        terminalDbId: this.deviceId
      }
    },
    // 编辑
    handleEdit(row) {
      this.jtChannel = row
    },
    // 编辑
    closeEdit(row) {
      this.jtChannel = null
    },
    // 编辑
    shooting(row) {
      // 文件下载地址
      const baseUrl = window.baseUrl ? window.baseUrl : ''
      const fileUrl = ((process.env.NODE_ENV === 'development') ? process.env.VUE_APP_BASE_API : baseUrl) + `/api/jt1078/snap?phoneNumber=${this.device.phoneNumber}&channelId=${row.channelId}`

      // 设置请求头
      const headers = new Headers()
      headers.append('access-token', this.$store.getters.token) // 设置授权头，替换YourAccessToken为实际的访问令牌
      // 发起  请求
      fetch(fileUrl, {
        method: 'GET',
        headers: headers
      })
        .then(response => response.blob())
        .then(blob => {
          console.log(blob)
          // 创建一个虚拟的链接元素，模拟点击下载
          const link = document.createElement('a')
          link.href = window.URL.createObjectURL(blob)
          link.download = `${this.device.phoneNumber}-${row.channelId}-${dayjs().format('YYYYMMDDHHmmss')}.jpg` // 设置下载文件名，替换filename.ext为实际的文件名和扩展名
          document.body.appendChild(link)

          // 模拟点击
          link.click()

          // 移除虚拟链接元素
          document.body.removeChild(link)
          this.$message.success('已申请截图', { closed: true })
        })
        .catch(error => console.error('下载失败：', error))
    }
  }
}
</script>
