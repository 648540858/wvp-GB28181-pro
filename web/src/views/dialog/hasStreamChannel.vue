<template>
  <div>
    <el-dialog
      v-if="showDialog"
      v-el-drag-dialog
      :visible.sync="showDialog"
      title="国标收流列表"
      width="70%"
      top="5rem"
      append-to-body
      :close-on-click-modal="false"
    >
      <el-form :inline="true" size="mini" @submit.native.prevent>
        <el-form-item label="搜索">
          <el-input
            v-model="query"
            placeholder="关键字"
            prefix-icon="el-icon-search"
            clearable
            @input="getChannelList"
          />
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="channelList" :height="500" stripe>
        <el-table-column prop="parentDeviceId" label="设备编号" min-width="180"/>
        <el-table-column prop="parentName" label="设备名称" min-width="180"/>
        <el-table-column prop="deviceId" label="通道编号" min-width="180"/>
        <el-table-column prop="name" label="通道名称" min-width="180"/>
        <el-table-column prop="ptzType" label="摄像头类型" min-width="100">
          <template v-slot:default="scope">
            <div>{{ scope.row.ptzTypeText }}</div>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="120" fixed="right">
          <template v-slot:default="scope">
            <el-button
              size="medium"
              icon="el-icon-video-play"
              type="text"
              :loading="scope.row.playing"
              @click="sendDevicePush(scope.row)"
            >播放
            </el-button>
            <el-button
              size="medium"
              icon="el-icon-switch-button"
              type="text"
              style="color: #f56c6c"
              @click="stopDevicePush(scope.row)"
            >停止
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        style="margin-top: 10px; text-align: right"
        :current-page="currentPage"
        :page-size="count"
        :page-sizes="[15, 25, 35, 50]"
        layout="total, sizes, prev, pager, next"
        :total="total"
        @size-change="handleSizeChange"
        @current-change="currentChange"
      />
    </el-dialog>

    <devicePlayer ref="devicePlayer"/>
  </div>
</template>

<script>
import devicePlayer from "@/views/dialog/devicePlayer";
import elDragDialog from "@/directive/el-drag-dialog";

export default {
  name: "HasStreamChannel",
  directives: {elDragDialog},
  components: {devicePlayer},
  data() {
    return {
      showDialog: false,
      loading: false,
      playing: false,
      channelList: [],
      query: null,
      currentPage: 1,
      count: 15,
      total: 0
    }
  },
  methods: {
    openDialog: function () {
      this.showDialog = true;
      this.getChannelList();
    },
    getChannelList: function () {
      this.loading = true;
      this.$store.dispatch("device/queryHasStreamChannels", {
        page: this.currentPage,
        count: this.count,
        query: this.query
      }).then((data) => {
        this.total = data.total
        this.channelList = data.list
      }).finally(() => {
        this.loading = false
      })
    },
    currentChange: function (val) {
      this.currentPage = val
      this.getChannelList()
    },
    handleSizeChange: function (val) {
      this.count = val
      this.getChannelList()
    },
    sendDevicePush: function (row) {
      const deviceId = row.parentDeviceId
      const channelId = row.deviceId
      this.$set(row, "playing", true)
      this.$store.dispatch("play/play", [deviceId, channelId])
        .then((data) => {
          this.$refs.devicePlayer.openDialog("media", deviceId, channelId, {
            streamInfo: data,
            hasAudio: row.hasAudio
          })
        })
        .finally(() => {
          this.$set(row, "playing", false)
        })
    },
    stopDevicePush: function (row) {
      this.$store.dispatch("play/stop", [row.parentDeviceId, row.deviceId]).then(_ => {
        this.getChannelList();
      }).catch((error) => {
        if (error.response.status === 402) {
          this.getChannelList();
        } else {
          this.$message.error({showClose: true, message: error})
        }
      })
    }
  }
}
</script>
