<template>
  <div id="app" class="app-container">
    <div style="height: calc(100vh - 124px);">
      <el-form :inline="true" size="mini">
        <el-form-item label="搜索">
          <el-input
            v-model="search"
            style="margin-right: 1rem; width: auto;"
            placeholder="关键字"
            prefix-icon="el-icon-search"
            clearable
            @input="initData"
          />
        </el-form-item>
        <el-form-item label="开始时间">
          <el-date-picker
            v-model="startTime"
            type="datetime"
            size="mini"
            value-format="yyyy-MM-dd HH:mm:ss"
            placeholder="选择日期时间"
            @change="initData"
          />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-date-picker
            v-model="endTime"
            type="datetime"
            size="mini"
            value-format="yyyy-MM-dd HH:mm:ss"
            placeholder="选择日期时间"
            @change="initData"
          />
        </el-form-item>
        <el-form-item label="节点选择">
          <el-select
            v-model="mediaServerId"
            size="mini"
            style="width: 16rem; margin-right: 1rem;"
            placeholder="请选择"
            @change="initData"
          >
            <el-option label="全部" value="" />
            <el-option
              v-for="item in mediaServerList"
              :key="item.id"
              :label="item.id"
              :value="item.id"
            />
          </el-select>
          <el-button
            icon="el-icon-delete"
            style="margin-right: 1rem;"
            :disabled="multipleSelection.length === 0"
            type="danger"
            @click="deleteRecord"
          >移除
          </el-button>
        </el-form-item>
        <el-form-item style="float: right;">
          <el-button icon="el-icon-refresh-right" circle :loading="loading" @click="initData()" />
        </el-form-item>
      </el-form>
      <!--设备列表-->
      <el-table :data="recordList" style="width: 100%" size="small" :loading="loading" height="calc(100% - 64px)" @selection-change="handleSelectionChange">
        <el-table-column
          type="selection"
          width="55"
        />
        <el-table-column prop="app" label="应用名" />
        <el-table-column prop="stream" label="流ID" width="380" />
        <el-table-column label="开始时间">
          <template v-slot:default="scope">
            {{ formatTimeStamp(scope.row.startTime) }}
          </template>
        </el-table-column>
        <el-table-column label="结束时间">
          <template v-slot:default="scope">
            {{ formatTimeStamp(scope.row.endTime) }}
          </template>
        </el-table-column>
        <el-table-column label="时长">
          <template v-slot:default="scope">
            <el-tag v-if="myServerId !== scope.row.serverId" style="border-color: #ecf1af">{{ formatTime(scope.row.timeLen) }}</el-tag>
            <el-tag v-if="myServerId === scope.row.serverId">{{ formatTime(scope.row.timeLen) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="fileName" label="文件名称" width="200" />
        <el-table-column prop="mediaServerId" label="流媒体" />
        <el-table-column label="操作" fixed="right" width="260">
          <template v-slot:default="scope">
            <el-button size="medium" icon="el-icon-video-play" type="text" @click="play(scope.row)">播放
            </el-button>
            <el-button size="medium" icon="el-icon-download" type="text" @click="downloadFile(scope.row)">下载
            </el-button>
            <el-button size="medium" icon="el-icon-info" type="text" @click="showDetail(scope.row)">详情
            </el-button>
            <el-button
              size="medium"
              icon="el-icon-delete"
              type="text"
              style="color: #f56c6c"
              @click="deleteOneRecord(scope.row)"
            >删除
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
    <el-dialog
      :title="playerTitle"
      :visible.sync="showPlayer"
      top="2rem"
      width="1200px"
      height="560px"
    >
      <h265web ref="recordVideoPlayer" :video-url="videoUrl" :height="false" :show-button="true" />
    </el-dialog>
  </div>
</template>

<script>
import h265web from '../common/h265web.vue'
import moment from 'moment'
import Vue from 'vue'

export default {
  name: 'CloudRecord',
  components: { h265web },
  data() {
    return {
      search: '',
      startTime: '',
      endTime: '',
      showPlayer: false,
      playerTitle: '',
      videoUrl: '',
      mediaServerList: [], // 滅体节点列表
      multipleSelection: [],
      mediaServerId: '', // 媒体服务
      mediaServerPath: null, // 媒体服务地址
      recordList: [], // 设备列表
      chooseRecord: null, // 媒体服务
      updateLooper: 0, // 数据刷新轮训标志
      currentPage: 1,
      count: 15,
      total: 0,
      loading: false

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
    this.getMediaServerList()
  },
  destroyed() {
    // this.$destroy('recordVideoPlayer')
  },
  methods: {
    initData: function() {
      this.getRecordList()
    },
    currentChange: function(val) {
      this.currentPage = val
      this.getRecordList()
    },
    handleSizeChange: function(val) {
      this.count = val
      this.getRecordList()
    },
    handleSelectionChange: function(val) {
      this.multipleSelection = val
    },
    getMediaServerList: function() {
      this.$store.dispatch('server/getOnlineMediaServerList')
        .then((data) => {
          this.mediaServerList = data
        })
    },
    getRecordList: function() {
      this.$store.dispatch('cloudRecord/queryList', {
        query: this.search,
        startTime: this.startTime,
        endTime: this.endTime,
        mediaServerId: this.mediaServerId,
        page: this.currentPage,
        count: this.count
      })
        .then((data) => {
          this.total = data.total
          this.recordList = data.list
        })
        .catch((error) => {
          console.log(error)
        })
        .finally(() => {
          this.loading = false
        })
    },
    play(row) {
      console.log(row)
      this.chooseRecord = row
      this.$store.dispatch('cloudRecord/getPlayPath', row.id)
        .then((data) => {
          if (location.protocol === 'https:') {
            this.videoUrl = data.httpsPath
          } else {
            this.videoUrl = data.httpPath
          }
          this.showPlayer = true
        })
        .catch((error) => {
          console.log(error)
        })
    },
    downloadFile(row) {
      this.$store.dispatch('cloudRecord/getPlayPath', row.id)
        .then((data) => {
          const link = document.createElement('a')
          link.target = '_blank'
          if (location.protocol === 'https:') {
            link.href = data.httpsPath + '&save_name=' + row.fileName
          } else {
            link.href = data.httpPath + '&save_name=' + row.fileName
          }
          link.click()
        })
        .catch((error) => {
          console.log(error)
        })
    },
    showDetail(row) {
      this.$router.push(`/cloudRecord/detail/${row.app}/${row.stream}`)
    },
    deleteRecord() {
      this.$confirm(`确定删除选中的${this.multipleSelection.length}个文件?`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        const ids = []
        for (let i = 0; i < this.multipleSelection.length; i++) {
          ids.push(this.multipleSelection[i].id)
        }
        this.$store.dispatch('cloudRecord/deleteRecord', ids)
          .then((data) => {
            this.$message.success({
              showClose: true,
              message: '删除成功'
            })
            this.getRecordList()
          })
      }).catch(() => {

      })
    },
    deleteOneRecord(row) {
      this.$confirm(`确定删除?`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        const ids = []
        ids.push(row.id)
        this.$store.dispatch('cloudRecord/deleteRecord', ids)
          .then((data) => {
            this.$message.success({
              showClose: true,
              message: '删除成功'
            })
            this.getRecordList()
          })
      }).catch(() => {

      })
    },
    formatTime(time) {
      const h = parseInt(time / 3600 / 1000)
      const minute = parseInt((time - h * 3600 * 1000) / 60 / 1000)
      let second = Math.ceil((time - h * 3600 * 1000 - minute * 60 * 1000) / 1000)
      if (second < 0) {
        second = 0
      }
      return (h > 0 ? h + `小时` : '') + (minute > 0 ? minute + '分' : '') + (second > 0 ? second + '秒' : '')
    },
    formatTimeStamp(time) {
      return moment.unix(time / 1000).format('yyyy-MM-DD HH:mm:ss')
    }
  }
}
</script>

<style>
.el-dialog__body {
  padding: 30px 0 !important;
}
</style>
