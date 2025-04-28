<template>
  <div id="channelList" style="height: calc(100vh - 124px);">
    <div v-if="!editId" style="height: 100%">
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
        <el-form-item label="通道类型">
          <el-select
            v-model="channelType"
            style="width: 8rem; margin-right: 1rem;"
            placeholder="请选择"
            default-first-option
            @change="search"
          >
            <el-option label="全部" value="" />
            <el-option label="设备" value="false" />
            <el-option label="子目录" value="true" />
          </el-select>
        </el-form-item>
        <el-form-item label="在线状态">
          <el-select
            v-model="online"
            style="width: 8rem; margin-right: 1rem;"
            placeholder="请选择"
            default-first-option
            @change="search"
          >
            <el-option label="全部" value="" />
            <el-option label="在线" value="true" />
            <el-option label="离线" value="false" />
          </el-select>
        </el-form-item>
        <el-form-item label="码流类型重置">
          <el-select
            v-model="subStream"
            style="width: 16rem; margin-right: 1rem;"
            placeholder="请选择码流类型"
            default-first-option
            @change="subStreamChange"
          >
            <el-option label="stream:0(主码流)" value="stream:0" />
            <el-option label="stream:1(子码流)" value="stream:1" />
            <el-option label="streamnumber:0(主码流-2022)" value="streamnumber:0" />
            <el-option label="streamnumber:1(子码流-2022)" value="streamnumber:1" />
            <el-option label="streamprofile:0(主码流-大华)" value="streamprofile:0" />
            <el-option label="streamprofile:1(子码流-大华)" value="streamprofile:1" />
            <el-option label="streamMode:main(主码流-水星+TP-LINK)" value="streamMode:main" />
            <el-option label="streamMode:sub(子码流-水星+TP-LINK)" value="streamMode:sub" />
          </el-select>
        </el-form-item>
        <el-form-item style="float: right;">
          <el-button icon="el-icon-refresh-right" circle @click="refresh()" />
        </el-form-item>
      </el-form>
      <el-table
        ref="channelListTable"
        size="small"
        :data="deviceChannelList"
        height="calc(100% - 64px)"
        style="width: 100%; font-size: 12px;"
        header-row-class-name="table-header"
      >
        <el-table-column prop="name" label="名称" min-width="180" />
        <el-table-column prop="deviceId" label="编号" min-width="180" />
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
        <!--          <el-table-column prop="subCount" label="子节点数" min-width="100">-->
        <!--          </el-table-column>-->
        <el-table-column prop="manufacturer" label="厂家" min-width="100" />
        <el-table-column label="位置信息" min-width="150">
          <template v-slot:default="scope">
            <span v-if="scope.row.longitude && scope.row.latitude">{{ scope.row.longitude }}<br>{{ scope.row.latitude }}</span>
            <span v-if="!scope.row.longitude || !scope.row.latitude">无</span>
          </template>
        </el-table-column>
        <el-table-column prop="ptzType" label="云台类型" min-width="100">
          <template v-slot:default="scope">
            <div>{{ scope.row.ptzTypeText }}</div>
          </template>
        </el-table-column>
        <el-table-column label="开启音频" min-width="100">
          <template v-slot:default="scope">
            <el-switch v-model="scope.row.hasAudio" active-color="#409EFF" @change="updateChannel(scope.row)" />
          </template>
        </el-table-column>
        <el-table-column label="码流类型" min-width="180">
          <template v-slot:default="scope">
            <el-select
              v-model="scope.row.streamIdentification"
              size="mini"
              style="margin-right: 1rem;"
              placeholder="请选择码流类型"
              default-first-option
              @change="channelSubStreamChange(scope.row)"
            >
              <el-option label="stream:0(主码流)" value="stream:0" />
              <el-option label="stream:1(子码流)" value="stream:1" />
              <el-option label="streamnumber:0(主码流-2022)" value="streamnumber:0" />
              <el-option label="streamnumber:1(子码流-2022)" value="streamnumber:1" />
              <el-option label="streamprofile:0(主码流-大华)" value="streamprofile:0" />
              <el-option label="streamprofile:1(子码流-大华)" value="streamprofile:1" />
              <el-option label="streamMode:main(主码流-水星+TP-LINK)" value="streamMode:main" />
              <el-option label="streamMode:sub(子码流-水星+TP-LINK)" value="streamMode:sub" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="状态" min-width="100">
          <template v-slot:default="scope">
            <div slot="reference" class="name-wrapper">
              <el-tag v-if="scope.row.status === 'ON'" size="medium">在线</el-tag>
              <el-tag v-if="scope.row.status !== 'ON'" size="medium" type="info">离线</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="340" fixed="right">
          <template v-slot:default="scope">
            <el-button
              size="medium"
              :disabled="device == null || device.online === 0"
              icon="el-icon-video-play"
              type="text"
              :loading="scope.row.playLoading"
              @click="sendDevicePush(scope.row)"
            >播放
            </el-button>
            <el-button
              v-if="!!scope.row.streamId"
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
            <el-button
              v-if="scope.row.subCount > 0 || scope.row.parental === 1 || scope.row.deviceId.length <= 8"
              size="medium"
              icon="el-icon-s-open"
              type="text"
              @click="changeSubchannel(scope.row)"
            >查看
            </el-button>
            <el-divider v-if="scope.row.subCount > 0 || scope.row.parental === 1 || scope.row.deviceId.length <= 8" direction="vertical" />
            <el-dropdown @command="(command)=>{moreClick(command, scope.row)}">
              <el-button size="medium" type="text">
                更多<i class="el-icon-arrow-down el-icon--right" />
              </el-button>
              <el-dropdown-menu>
                <el-dropdown-item command="records" :disabled="device == null || device.online === 0">
                  设备录像</el-dropdown-item>
                <el-dropdown-item command="cloudRecords" :disabled="device == null || device.online === 0">
                  云端录像</el-dropdown-item>
                <el-dropdown-item command="record" :disabled="device == null || device.online === 0">
                  设备录像控制-开始</el-dropdown-item>
                <el-dropdown-item command="stopRecord" :disabled="device == null || device.online === 0">
                  设备录像控制-停止</el-dropdown-item>
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
    </div>

    <devicePlayer ref="devicePlayer" />
    <channel-edit v-if="editId" :id="editId" :close-edit="closeEdit" />

  </div>
</template>

<script>
import devicePlayer from '../../dialog/devicePlayer.vue'
import Edit from './edit.vue'

export default {
  name: 'ChannelList',
  components: {
    devicePlayer,
    ChannelEdit: Edit
  },
  props: {
    defaultPage: {
      type: Number,
      default: 1
    },
    defaultCount: {
      type: Number,
      default: 15
    },
    deviceId: {
      type: String,
      default: null
    },
    parentChannelId: {
      type: String || null,
      default: null
    }
  },
  data() {
    return {
      device: null,
      deviceChannelList: [],
      videoComponentList: [],
      currentPlayerInfo: {}, // 当前播放对象
      updateLooper: 0, // 数据刷新轮训标志
      searchSrt: '',
      channelType: '',
      online: '',
      subStream: '',
      winHeight: window.innerHeight - 200,
      currentPage: this.defaultPage | 1,
      count: this.defaultCount | 15,
      total: 0,
      beforeUrl: '/device',
      editId: null,
      loadSnap: {},
      ptzTypes: {
        0: '未知',
        1: '球机',
        2: '半球',
        3: '固定枪机',
        4: '遥控枪机'
      }
    }
  },
  watch: {
    deviceId: function(val) {
      this.$store.dispatch('device/queryDeviceOne', this.deviceId)
        .then(data => {
          this.device = data
        })
      this.initData()
    }
  },
  mounted() {
    console.log(23222)
    if (this.deviceId) {
      this.$store.dispatch('device/queryDeviceOne', this.deviceId)
        .then(data => {
          this.device = data
        })
    }
    this.initData()
  },
  destroyed() {
    this.$destroy('videojs')
    clearTimeout(this.updateLooper)
  },
  methods: {
    initData: function() {
      if (this.parentChannelId === null || typeof (this.parentChannelId) === 'undefined' || this.parentChannelId === 0) {
        this.getDeviceChannelList()
      } else {
        this.showSubChannels()
      }
    },
    initParam: function() {
      this.deviceId = this.$route.params.deviceId
      this.parentChannelId = this.$route.params.parentChannelId
      this.currentPage = 1
      this.count = 15
      if (this.parentChannelId === '' || this.parentChannelId === 0) {
        this.beforeUrl = '/device/list'
      }
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
      console.log(this.deviceId)
      if (typeof (this.deviceId) === 'undefined') return
      this.$store.dispatch('device/queryChannels', [this.deviceId, {
        page: this.currentPage,
        count: this.count,
        query: this.searchSrt,
        online: this.online,
        channelType: this.channelType
      }]).then(data => {
        this.total = data.total
        this.deviceChannelList = data.list
        this.deviceChannelList.forEach(e => {
          e.ptzType = e.ptzType + ''
          this.$set(e, 'playLoading', false)
        })
        // 防止出现表格错位
        this.$nextTick(() => {
          this.$refs.channelListTable.doLayout()
        })
      })
    },

    // 通知设备上传媒体流
    sendDevicePush: function(itemData) {
      const deviceId = this.deviceId
      const channelId = itemData.deviceId
      itemData.playLoading = true
      console.log('通知设备推流1：' + deviceId + ' : ' + channelId)
      this.$store.dispatch('play/play', [deviceId, channelId])
        .then((data) => {
          setTimeout(() => {
            const snapId = deviceId + '_' + channelId
            this.loadSnap[deviceId + channelId] = 0
            this.getSnapErrorEvent(snapId)
          }, 5000)
          itemData.streamId = data.stream
          this.$refs.devicePlayer.openDialog('media', deviceId, channelId, {
            streamInfo: data,
            hasAudio: itemData.hasAudio
          })
          setTimeout(() => {
            this.initData()
          }, 1000)
        }).finally(() => {
          itemData.playLoading = false
        })
    },
    moreClick: function(command, itemData) {
      if (command === 'records') {
        this.queryRecords(itemData)
      } else if (command === 'cloudRecords') {
        this.queryCloudRecords(itemData)
      } else if (command === 'record') {
        this.startRecord(itemData)
      } else if (command === 'stopRecord') {
        this.stopRecord(itemData)
      }
    },
    queryRecords: function(itemData) {
      const deviceId = this.deviceId
      const channelId = itemData.deviceId

      this.$router.push(`/device/record/${deviceId}/${channelId}`)
    },
    queryCloudRecords: function(itemData) {
      const deviceId = this.deviceId
      const channelId = itemData.deviceId

      this.$router.push(`/cloudRecord/detail/rtp/${deviceId}_${channelId}`)
    },
    startRecord: function(itemData) {
      this.$store.dispatch('device/deviceRecord', {
        deviceId: this.deviceId,
        channelId: itemData.deviceId,
        recordCmdStr: 'Record'
      }).then(data => {
        this.$message.success({
          showClose: true,
          message: '开始录像成功'
        })
      }).catch((error) => {
        this.$message.error({
          showClose: true,
          message: error.message
        })
      })
    },
    stopRecord: function(itemData) {
      this.$store.dispatch('device/deviceRecord', {
        deviceId: this.deviceId,
        channelId: itemData.deviceId,
        recordCmdStr: 'StopRecord'
      }).then(data => {
        this.$message.success({
          showClose: true,
          message: '停止录像成功'
        })
      }).catch((error) => {
        this.$message.error({
          showClose: true,
          message: error.message
        })
      })
    },
    stopDevicePush: function(itemData) {
      this.$store.dispatch('play/stop', [this.deviceId, itemData.deviceId]).then(data => {
        this.initData()
      }).catch((error) => {
        if (error.response.status === 402) { // 已经停止过
          this.initData()
        } else {
          console.log(error)
        }
      })
    },
    getSnap: function(row) {
      const baseUrl = window.baseUrl ? window.baseUrl : ''
      return ((process.env.NODE_ENV === 'development') ? process.env.VUE_APP_BASE_API : baseUrl) + '/api/device/query/snap/' + this.deviceId + '/' + row.deviceId
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
          const url = (process.env.NODE_ENV === 'development' ? 'debug' : '') + '/api/device/query/snap/' + deviceId + '/' + channelId
          this.loadSnap[deviceId + channelId]++
          document.getElementById(deviceId + channelId).setAttribute('src', url + '?' + new Date().getTime())
        }, 1000)
      }
    },
    showDevice: function() {
      // this.$router.push(this.beforeUrl).then(() => {
      //   this.initParam()
      //   this.initData()
      // })
      this.$emit('show-device')
    },
    changeSubchannel(itemData) {
      this.beforeUrl = this.$router.currentRoute.path

      var url = `/${this.$router.currentRoute.name}/${this.$router.currentRoute.params.deviceId}/${itemData.deviceId}`
      this.$router.push(url).then(() => {
        this.searchSrt = ''
        this.channelType = ''
        this.online = ''
        this.initParam()
        this.initData()
      })
    },
    showSubChannels: function() {
      this.$store.dispatch('device/querySubChannels', [
        {
          page: this.currentPage,
          count: this.count,
          query: this.searchSrt,
          online: this.online,
          channelType: this.channelType
        },
        this.deviceId,
        this.parentChannelId
      ])
        .then(data => {
          this.total = data.total
          this.deviceChannelList = data.list
          this.deviceChannelList.forEach(e => {
            e.ptzType = e.ptzType + ''
          })
          // 防止出现表格错位
          this.$nextTick(() => {
            this.$refs.channelListTable.doLayout()
          })
        })
    },
    search: function() {
      this.currentPage = 1
      this.total = 0
      this.initData()
    },
    updateChannel: function(row) {
      this.$store.dispatch('device/changeChannelAudio', {
        channelId: row.id,
        audio: row.hasAudio
      })
    },
    subStreamChange: function() {
      this.$confirm('确定重置所有通道的码流类型?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.$store.dispatch('device/updateChannelStreamIdentification', {
          deviceDbId: this.device.id,
          streamIdentification: this.subStream
        })
          .then(data => {
            this.initData()
          })
          .finally(() => {
            this.subStream = ''
          })
      }).catch(() => {
        this.subStream = ''
      })
    },
    channelSubStreamChange: function(row) {
      this.$store.dispatch('device/updateChannelStreamIdentification', {
        deviceDbId: row.deviceDbId,
        id: row.id,
        streamIdentification: row.streamIdentification
      })
        .then(data => {
          this.initData()
        })
        .finally(() => {
          this.subStream = ''
        })
    },
    refresh: function() {
      this.initData()
    },
    // 编辑
    handleEdit(row) {
      this.editId = row.id
    },
    // 结束编辑
    closeEdit: function() {
      this.editId = null
      this.getDeviceChannelList()
    }

  }
}
</script>
