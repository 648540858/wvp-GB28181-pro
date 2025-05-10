<template>
  <div id="pushList" class="app-container">
    <div v-if="!streamPush" style="height: calc(100vh - 124px);">
      <el-form :inline="true" size="mini">
        <el-form-item label="搜索">
          <el-input
            v-model="searchSrt"
            style="margin-right: 1rem; width: auto;"
            placeholder="关键字"
            prefix-icon="el-icon-search"
            clearable
            @input="getPushList"
          />
        </el-form-item>
        <el-form-item label="流媒体">
          <el-select
            v-model="mediaServerId"
            style="margin-right: 1rem;"
            placeholder="请选择"
            default-first-option
            @change="getPushList"
          >
            <el-option label="全部" value="" />
            <el-option
              v-for="item in mediaServerList"
              :key="item.id"
              :label="item.id"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="推流状态">
          <el-select
            v-model="pushing"
            style="margin-right: 1rem;"
            placeholder="请选择"
            default-first-option
            @change="getPushList"
          >
            <el-option label="全部" value="" />
            <el-option label="推流中" value="true" />
            <el-option label="已停止" value="false" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button icon="el-icon-plus" style="margin-right: 1rem;" type="primary" @click="addStream">添加
          </el-button>
          <el-button icon="el-icon-upload2" style="margin-right: 1rem;" @click="importChannel">
            通道导入
          </el-button>
          <el-button icon="el-icon-download" style="margin-right: 1rem;">
            <a
              style="text-align: center; text-decoration: none"
              href="/static/file/推流通道导入.zip"
              download="推流通道导入.zip"
            >下载模板</a>
          </el-button>
          <el-button
            icon="el-icon-delete"
            style="margin-right: 1rem;"
            :disabled="multipleSelection.length === 0"
            type="danger"
            @click="batchDel"
          >移除
          </el-button>
        </el-form-item>
        <el-form-item style="float: right;">
          <el-button icon="el-icon-refresh-right" circle @click="refresh()" />
        </el-form-item>
      </el-form>
      <el-table
        ref="pushListTable"
        size="small"
        :data="pushList"
        style="width: 100%"
        height="calc(100% - 64px)"
        :loading="loading"
        :row-key="(row)=> row.app + row.stream"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" :reserve-selection="true" min-width="55" />
        <el-table-column prop="gbName" label="名称" min-width="150" />
        <el-table-column prop="app" label="应用名" min-width="100" />
        <el-table-column prop="stream" label="流ID" min-width="200" />
        <el-table-column label="推流状态" min-width="100">
          <template v-slot:default="scope">
            <el-tag v-if="scope.row.pushing && $myServerId !== scope.row.serverId" size="medium" style="border-color: #ecf1af">推流中</el-tag>
            <el-tag v-if="scope.row.pushing && $myServerId === scope.row.serverId" size="medium">推流中</el-tag>
            <el-tag v-if="!scope.row.pushing" size="medium" type="info">已停止</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="gbDeviceId" label="国标编码" min-width="200" />
        <el-table-column label="位置信息" min-width="150">
          <template v-slot:default="scope">
            <span v-if="scope.row.gbLongitude && scope.row.gbLatitude" size="medium">{{ scope.row.gbLongitude }}<br>{{ scope.row.gbLatitude }}</span>
            <span v-if="!scope.row.gbLongitude || !scope.row.gbLatitude" size="medium">无</span>
          </template>
        </el-table-column>
        <el-table-column prop="mediaServerId" label="流媒体" min-width="150" />
        <el-table-column label="开始时间" min-width="150">
          <template v-slot:default="scope">
            <el-button-group>
              {{ scope.row.pushTime == null? "-":scope.row.pushTime }}
            </el-button-group>
          </template>
        </el-table-column>

        <el-table-column label="操作" min-width="300" fixed="right">
          <template v-slot:default="scope">
            <el-button size="medium" :loading="scope.row.playLoading" icon="el-icon-video-play" type="text" @click="playPush(scope.row)">播放
            </el-button>
            <el-divider direction="vertical" />
            <el-button size="medium" icon="el-icon-delete" type="text" style="color: #f56c6c" @click="deletePush(scope.row.id)">删除</el-button>
            <el-divider direction="vertical" />
            <el-button size="medium" icon="el-icon-position" type="text" @click="edit(scope.row)">
              编辑
            </el-button>
            <el-button size="medium" icon="el-icon-cloudy" type="text" @click="queryCloudRecords(scope.row)">云端录像
            </el-button>
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
    <addStreamTOGB ref="addStreamTOGB" />
    <importChannel ref="importChannel" />
    <stream-push-edit v-if="streamPush" :stream-push="streamPush" :close-edit="closeEdit" style="height: calc(100vh - 90px);" />
  </div>
</template>

<script>
import devicePlayer from '../dialog/devicePlayer.vue'
import addStreamTOGB from '../dialog/pushStreamEdit.vue'
import importChannel from '../dialog/importChannel.vue'
import StreamPushEdit from './edit.vue'

export default {
  name: 'PushList',
  components: {
    StreamPushEdit,
    devicePlayer,
    addStreamTOGB,
    importChannel
  },
  data() {
    return {
      pushList: [], // 设备列表
      currentPusher: {}, // 当前操作设备对象
      updateLooper: 0, // 数据刷新轮训标志
      currentDeviceChannelsLenth: 0,
      currentPage: 1,
      count: 15,
      total: 0,
      searchSrt: '',
      pushing: '',
      mediaServerId: '',
      mediaServerList: [],
      multipleSelection: [],
      loading: false,
      streamPush: null
    }
  },
  mounted() {
    this.initData()
    this.updateLooper = setInterval(this.getPushList, 2000)
  },
  destroyed() {
    clearTimeout(this.updateLooper)
  },
  methods: {
    initData: function() {
      this.loading = true
      this.$store.dispatch('server/getMediaServerList')
        .then((data) => {
          this.mediaServerList = data
        })
      this.getPushList()
    },
    currentChange: function(val) {
      this.currentPage = val
      this.getPushList()
    },
    handleSizeChange: function(val) {
      this.count = val
      this.getPushList()
    },
    getPushList: function() {
      this.$store.dispatch('streamPush/queryList', {
        page: this.currentPage,
        count: this.count,
        query: this.searchSrt,
        pushing: this.pushing,
        mediaServerId: this.mediaServerId
      })
        .then((data) => {
          this.total = data.total
          this.pushList = data.list
          this.pushList.forEach(e => {
            this.$set(e, 'location', '')
            this.$set(e, 'playLoading', false)
            if (e.gbLongitude && e.gbLatitude) {
              this.$set(e, 'location', e.gbLongitude + ',' + e.gbLatitude)
            }
          })
        })
        .finally(() => {
          this.loading = false
        })
    },

    playPush: function(row) {
      row.playLoading = true
      this.$store.dispatch('streamPush/play', row.id)
        .then((data) => {
          this.$refs.devicePlayer.openDialog('streamPlay', null, null, {
            streamInfo: data,
            hasAudio: true
          })
        })
        .finally(() => {
          row.playLoading = false
        })
    },
    deletePush: function(id) {
      this.$confirm(`确定删除通道?`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.loading = true
        this.$store.dispatch('streamPush/remove', id)
          .then((data) => {
            this.initData()
          })
      }).catch(() => {

      })
    },
    edit: function(row) {
      this.streamPush = row
    },
    // 结束编辑
    closeEdit: function() {
      this.streamPush = null
      this.getPushList()
    },
    queryCloudRecords: function(row) {
      this.$router.push(`/cloudRecordDetail/${row.app}/${row.stream}`)
    },
    importChannel: function() {
      this.$refs.importChannel.openDialog(() => {})
    },
    addStream: function() {
      this.streamPush = {}
    },
    batchDel: function() {
      this.$confirm(`确定删除选中的${this.multipleSelection.length}个通道?`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        const ids = []
        for (let i = 0; i < this.multipleSelection.length; i++) {
          ids.push(this.multipleSelection[i].id)
        }
        this.$store.dispatch('streamPush/batchRemove', ids)
          .then((data) => {
            this.initData()
            this.$refs.pushListTable.clearSelection()
          })
      }).catch(() => {

      })
    },
    handleSelectionChange: function(val) {
      this.multipleSelection = val
    },
    refresh: function() {
      this.initData()
    }
  }
}
</script>

