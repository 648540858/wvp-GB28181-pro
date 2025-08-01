<template>
  <div id="channelList" class="app-container" style="height: calc(100vh - 124px);">
    <div v-if="!editId" style="height: 100%">
      <el-form :inline="true" size="mini">
        <el-form-item label="搜索">
          <el-input
            v-model="searchStr"
            style="margin-right: 1rem; width: auto;"
            placeholder="关键字"
            prefix-icon="el-icon-search"
            clearable
            @input="search"
          />
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
        <el-form-item label="类型">
          <el-select
            v-model="channelType"
            style="width: 8rem; margin-right: 1rem;"
            placeholder="请选择"
            default-first-option
            @change="getChannelList"
          >
            <el-option label="全部" value="" />
            <el-option v-for="item in Object.values($channelTypeList)" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item style="float: right;">
          <el-button icon="el-icon-refresh-right" circle @click="refresh()" title="刷新表格"/>
        </el-form-item>
      </el-form>
      <el-table
        ref="channelListTable"
        size="small"
        :data="channelList"
        height="calc(100% - 64px)"
        style="width: 100%; font-size: 12px;"
        header-row-class-name="table-header"
      >
        <el-table-column prop="gbName" label="名称" min-width="180" />
        <el-table-column prop="gbDeviceId" label="编号" min-width="180" />
        <el-table-column prop="gbManufacturer" label="厂家" min-width="100" />
        <el-table-column label="类型" min-width="100">
          <template v-slot:default="scope">
            <div slot="reference" class="name-wrapper">
              <el-tag size="medium" effect="plain" type="success" :style="$channelTypeList[scope.row.dataType].style">{{ $channelTypeList[scope.row.dataType].name }}</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="位置信息" min-width="150">
          <template v-slot:default="scope">
            <span v-if="scope.row.gbLongitude && scope.row.gbLatitude">{{ scope.row.gbLongitude }}<br>{{ scope.row.gbLatitude }}</span>
            <span v-if="!scope.row.gbLongitude || !scope.row.gbLatitude">无</span>
          </template>
        </el-table-column>
        <el-table-column prop="ptzType" label="云台类型" min-width="100">
          <template v-slot:default="scope">
            <div>{{ scope.row.ptzTypeText }}</div>
          </template>
        </el-table-column>
        <el-table-column label="状态" min-width="100">
          <template v-slot:default="scope">
            <div slot="reference" class="name-wrapper">
              <el-tag v-if="scope.row.gbStatus === 'ON'" size="medium">在线</el-tag>
              <el-tag v-if="scope.row.gbStatus !== 'ON'" size="medium" type="info">离线</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="210" fixed="right">
          <template v-slot:default="scope">
            <el-button
              size="medium"
              :disabled="scope.row.gbStatus !== 'ON'"
              icon="el-icon-video-play"
              type="text"
              :loading="scope.row.playLoading"
              @click="sendDevicePush(scope.row)"
            >播放
            </el-button>
            <el-button
              v-if="!!scope.row.streamId"
              size="medium"
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
              v-if="$store.getters.authority !== 2"
              @click="handleEdit(scope.row)"
            >
              编辑
            </el-button>
            <el-divider direction="vertical" />
            <el-dropdown @command="(command)=>{moreClick(command, scope.row)}">
              <el-button size="medium" type="text">
                更多<i class="el-icon-arrow-down el-icon--right" />
              </el-button>
              <el-dropdown-menu>
                <el-dropdown-item command="records" :disabled="scope.row.gbStatus !== 'ON'">
                  设备录像</el-dropdown-item>
                <el-dropdown-item command="cloudRecords" :disabled="scope.row.gbStatus !== 'ON'">
                  云端录像</el-dropdown-item>
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
import devicePlayer from '@/views/common/channelPlayer/index.vue'
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
    }
  },
  computed: {
    excelName(){
      return '通道列表-' + this.currentPage
    }
  },
  data() {
    return {
      device: null,
      channelList: [],
      excelFields: {
        名称: 'gbName',
        编号: 'gbDeviceId',
        厂家: 'gbManufacturer',
        类型: {
          field: 'dataType',
          callback: (value) => {
            return this.$channelTypeList[value].name
          }
        },
        经度: 'gbLongitude',
        纬度: 'gbLatitude',
        云台类型: 'ptzTypeText',
        状态: {
          field: 'gbStatus',
          callback: (value) => {
            return value === 'ON' ? '在线' : '离线'
          }
        }
      },
      videoComponentList: [],
      currentPlayerInfo: {}, // 当前播放对象
      updateLooper: 0, // 数据刷新轮训标志
      searchStr: '',
      channelType: '',
      online: 'true',
      subStream: '',
      winHeight: window.innerHeight - 200,
      currentPage: this.defaultPage | 1,
      count: this.defaultCount | 15,
      total: 0,
      beforeUrl: '/device',
      editId: null
    }
  },
  mounted() {
    this.initData()
  },
  destroyed() {
    this.$destroy('videojs')
    clearTimeout(this.updateLooper)
  },
  methods: {
    initData: function() {
      this.getChannelList()
    },
    initParam: function() {
      this.currentPage = 1
      this.count = 15
    },
    currentChange: function(val) {
      this.currentPage = val
      this.initData()
    },
    handleSizeChange: function(val) {
      this.count = val
      this.getChannelList()
    },
    getChannelList: function() {
      this.$store.dispatch('commonChanel/getList', {
        page: this.currentPage,
        count: this.count,
        query: this.searchStr,
        online: this.online,
        channelType: this.channelType
      }).then(data => {
        this.total = data.total
        this.channelList = data.list
        this.channelList.forEach(e => {
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
      itemData.playLoading = true
      this.$store.dispatch('commonChanel/playChannel', itemData.gbId)
        .then((data) => {
          itemData.streamId = data.stream
          this.$refs.devicePlayer.openDialog('media', itemData.gbId, {
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
    queryRecords: function(itemData) {
      const channelId = itemData.gbId
      this.$router.push(`/channel/record/${channelId}`)
    },
    queryCloudRecords: function(itemData) {
      const deviceId = this.deviceId
      const channelId = itemData.deviceId

      this.$router.push(`/cloudRecord/detail/rtp/${deviceId}_${channelId}`)
    },
    stopDevicePush: function(itemData) {
      this.$store.dispatch('commonChanel/stopPlayChannel', itemData.gbId).then(data => {
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
    search: function() {
      this.currentPage = 1
      this.total = 0
      this.initData()
    },
    refresh: function() {
      this.initData()
    },
    // 编辑
    handleEdit(row) {
      console.log(row)
      this.editId = row.gbId
    },
    // 结束编辑
    closeEdit: function() {
      this.editId = null
      this.getChannelList()
    },
    moreClick: function(command, itemData) {
      if (command === 'records') {
        this.queryRecords(itemData)
      } else if (command === 'cloudRecords') {
        this.queryCloudRecords(itemData)
      }
    }
  }
}
</script>
