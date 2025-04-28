<template>
  <div id="streamProxyList" class="app-container">
    <div v-if="!streamProxy" style="height: calc(100vh - 124px);">
      <el-form :inline="true" size="mini">
        <el-form-item label="搜索">
          <el-input
            v-model="searchSrt"
            style="margin-right: 1rem; width: auto;"
            placeholder="关键字"
            prefix-icon="el-icon-search"
            clearable
            @input="getStreamProxyList"
          />
        </el-form-item>
        <el-form-item label="流媒体">
          <el-select
            v-model="mediaServerId"
            style="margin-right: 1rem;"
            placeholder="请选择"
            default-first-option
            @change="getStreamProxyList"
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
        <el-form-item label="拉流状态">
          <el-select
            v-model="pulling"
            style="margin-right: 1rem;"
            placeholder="请选择"
            default-first-option
            @change="getStreamProxyList"
          >
            <el-option label="全部" value="" />
            <el-option label="正在拉流" value="true" />
            <el-option label="尚未拉流" value="false" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button icon="el-icon-plus" size="mini" style="margin-right: 1rem;" type="primary" @click="addStreamProxy">添加代理</el-button>
        </el-form-item>
        <el-form-item style="float: right;">
          <el-button icon="el-icon-refresh-right" circle @click="refresh()" />
        </el-form-item>
      </el-form>
      <devicePlayer ref="devicePlayer" />
      <el-table size="small" :data="streamProxyList" style="width: 100%" height="calc(100% - 64px)">
        <el-table-column prop="app" label="流应用名" min-width="120" show-overflow-tooltip />
        <el-table-column prop="stream" label="流ID" min-width="120" show-overflow-tooltip />
        <el-table-column label="流地址" min-width="250" show-overflow-tooltip>
          <template v-slot:default="scope">
            <div slot="reference" class="name-wrapper">
              <el-tag v-clipboard="scope.row.srcUrl" size="medium" @success="$message({type:'success', message:'成功拷贝到粘贴板'})">
                <i class="el-icon-document-copy" title="点击拷贝" />
                {{ scope.row.srcUrl }}
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="mediaServerId" label="流媒体" min-width="180" />
        <el-table-column label="代理方式" width="100">
          <template v-slot:default="scope">
            <div slot="reference" class="name-wrapper">
              {{ scope.row.type === "default"? "默认":"FFMPEG代理" }}
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="gbDeviceId" label="国标编码" min-width="180" show-overflow-tooltip />
        <el-table-column label="拉流状态" min-width="100">
          <template v-slot:default="scope">
            <div slot="reference" class="name-wrapper">
              <el-tag v-if="scope.row.pulling && myServerId !== scope.row.serverId" size="medium" style="border-color: #ecf1af">正在拉流</el-tag>
              <el-tag v-if="scope.row.pulling && myServerId === scope.row.serverId" size="medium">正在拉流</el-tag>
              <el-tag v-if="!scope.row.pulling" size="medium" type="info">尚未拉流</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="启用" min-width="100">
          <template v-slot:default="scope">
            <div slot="reference" class="name-wrapper">
              <el-tag v-if="scope.row.enable && myServerId !== scope.row.serverId" size="medium" style="border-color: #ecf1af">已启用</el-tag>
              <el-tag v-if="scope.row.enable && myServerId === scope.row.serverId" size="medium">已启用</el-tag>
              <el-tag v-if="!scope.row.enable" size="medium" type="info">未启用</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" min-width="150" show-overflow-tooltip />
        <el-table-column label="操作" width="370" fixed="right">
          <template v-slot:default="scope">
            <el-button size="medium" :loading="scope.row.playLoading" icon="el-icon-video-play" type="text" @click="play(scope.row)">播放</el-button>
            <el-divider direction="vertical" />
            <el-button v-if="scope.row.pulling" size="medium" icon="el-icon-switch-button" style="color: #f56c6c" type="text" @click="stopPlay(scope.row)">停止</el-button>
            <el-divider v-if="scope.row.pulling" direction="vertical" />
            <el-button size="medium" icon="el-icon-edit" type="text" @click="edit(scope.row)">
              编辑
            </el-button>
            <el-divider direction="vertical" />
            <el-button size="medium" icon="el-icon-cloudy" type="text" @click="queryCloudRecords(scope.row)">云端录像</el-button>
            <el-divider direction="vertical" />
            <el-button size="medium" icon="el-icon-delete" type="text" style="color: #f56c6c" @click="deleteStreamProxy(scope.row)">删除</el-button>
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
    <StreamProxyEdit v-if="streamProxy" v-model="streamProxy" :close-edit="closeEdit" />
  </div>
</template>

<script>
import devicePlayer from '../dialog/devicePlayer.vue'
import StreamProxyEdit from './edit.vue'
import Vue from 'vue'

export default {
  name: 'Proxy',
  components: {
    devicePlayer,
    StreamProxyEdit
  },
  data() {
    return {
      streamProxyList: [],
      currentPusher: {}, // 当前操作设备对象
      updateLooper: 0, // 数据刷新轮训标志
      currentDeviceChannelsLenth: 0,
      currentPage: 1,
      count: 15,
      total: 0,
      streamProxy: null,
      searchSrt: '',
      mediaServerId: '',
      pulling: '',
      mediaServerList: []
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
    this.startUpdateList()
  },
  destroyed() {
    this.$destroy('videojs')
    clearTimeout(this.updateLooper)
  },
  methods: {
    initData: function() {
      this.getStreamProxyList()
      this.$store.dispatch('server/getOnlineMediaServerList')
        .then((data) => {
          this.mediaServerList = data
        })
    },
    startUpdateList: function() {
      this.updateLooper = setInterval(() => {
        if (!this.streamProxy) {
          this.getStreamProxyList()
        }
      }, 1000)
    },
    currentChange: function(val) {
      this.currentPage = val
      this.getStreamProxyList()
    },
    handleSizeChange: function(val) {
      this.count = val
      this.getStreamProxyList()
    },
    getStreamProxyList: function() {
      this.$store.dispatch('streamProxy/queryList', {
        page: this.currentPage,
        count: this.count,
        query: this.searchSrt,
        pulling: this.pulling,
        mediaServerId: this.mediaServerId
      })
        .then(data => {
          this.total = data.total
          for (let i = 0; i < data.list.length; i++) {
            data.list[i]['playLoading'] = false
          }
          this.streamProxyList = data.list
        })
    },
    addStreamProxy: function() {
      this.streamProxy = {
        type: 'default',
        dataType: 3,
        noneReader: 1,
        enable: true,
        enableAudio: true,
        mediaServerId: '',
        timeout: 10
      }
    },
    edit: function(row) {
      if (row.enableDisableNoneReader) {
        this.$set(row, 'noneReader', 1)
      } else if (row.enableRemoveNoneReader) {
        this.$set(row, 'noneReader', 2)
      } else {
        this.$set(row, 'noneReader', 0)
      }
      this.streamProxy = row
      this.$set(this.streamProxy, 'rtspType', row.rtspType)
    },
    closeEdit: function(row) {
      this.streamProxy = null
    },
    play: function(row) {
      row.playLoading = true
      this.$store.dispatch('streamProxy/play', row.id)
        .then((data) => {
          this.$refs.devicePlayer.openDialog('streamPlay', null, null, {
            streamInfo: data,
            hasAudio: true
          })
        })
        .catch((error) => {
          console.log(error)
        })
        .finally(() => {
          row.playLoading = false
        })
    },
    stopPlay: function(row) {
      this.$store.dispatch('streamProxy/stopPlay', row.id)
        .then((data) => {
          this.$refs.devicePlayer.openDialog('streamPlay', null, null, {
            streamInfo: data,
            hasAudio: true
          })
        })
        .catch((error) => {
          console.log(error)
        })
    },
    queryCloudRecords: function(row) {
      this.$router.push(`/cloudRecordDetail/${row.app}/${row.stream}`)
    },
    deleteStreamProxy: function(row) {
      this.$confirm('确定删除此代理吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.$store.dispatch('streamProxy/remove', row.id)
          .then((data) => {
            this.$message.success({
              showClose: true,
              message: '删除成功'
            })
            this.initData()
          })
      }).catch(() => {
      })
    },
    refresh: function() {
      this.initData()
    }
  }
}
</script>
