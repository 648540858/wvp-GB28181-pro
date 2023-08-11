<template>
  <div id="pushVideoList" style="width: 100%">
    <div class="page-header">
      <div class="page-title">推流列表</div>
      <div class="page-header-btn">
        搜索:
        <el-input @input="getPushList" style="margin-right: 1rem; width: auto;" size="mini" placeholder="关键字"
                  prefix-icon="el-icon-search" v-model="searchSrt" clearable></el-input>
        流媒体:
        <el-select size="mini" @change="getPushList" style="margin-right: 1rem;" v-model="mediaServerId"
                   placeholder="请选择" default-first-option>
          <el-option label="全部" value=""></el-option>
          <el-option
            v-for="item in mediaServerList"
            :key="item.id"
            :label="item.id"
            :value="item.id">
          </el-option>
        </el-select>
        推流状态:
        <el-select size="mini" style="margin-right: 1rem;" @change="getPushList" v-model="pushing" placeholder="请选择"
                   default-first-option>
          <el-option label="全部" value=""></el-option>
          <el-option label="推流进行中" value="true"></el-option>
          <el-option label="推流未进行" value="false"></el-option>
        </el-select>
        <el-button icon="el-icon-upload2" size="mini" style="margin-right: 1rem;" type="primary" @click="importChannel">
          通道导入
        </el-button>
        <el-button icon="el-icon-download" size="mini" style="margin-right: 1rem;" type="primary">
          <a style="color: #FFFFFF; text-align: center; text-decoration: none" href="/static/file/推流通道导入.zip"
             download='推流通道导入.zip'>下载模板</a>
        </el-button>
        <el-button icon="el-icon-delete" size="mini" style="margin-right: 1rem;"
                   :disabled="multipleSelection.length === 0" type="danger" @click="batchDel">批量移除
        </el-button>
        <el-button icon="el-icon-plus" size="mini" style="margin-right: 1rem;" type="primary" @click="addStream">添加通道
        </el-button>
        <el-button icon="el-icon-refresh-right" circle size="mini" @click="refresh()"></el-button>
      </div>
    </div>
    <devicePlayer ref="devicePlayer"></devicePlayer>
    <addStreamTOGB ref="addStreamTOGB"></addStreamTOGB>
    <el-table ref="pushListTable" :data="pushList" style="width: 100%" :height="winHeight"
              @selection-change="handleSelectionChange" :row-key="(row)=> row.app + row.stream">
      <el-table-column  type="selection" :reserve-selection="true" min-width="55">
      </el-table-column>
      <el-table-column prop="name" label="名称" min-width="200">
      </el-table-column>
      <el-table-column prop="app" label="APP" min-width="200">
      </el-table-column>
      <el-table-column prop="stream" label="流ID" min-width="200">
      </el-table-column>
      <el-table-column prop="gbId" label="国标编码" min-width="200" >
      </el-table-column>
      <el-table-column prop="mediaServerId" label="流媒体" min-width="200" >
      </el-table-column>
      <el-table-column label="开始时间"  min-width="200">
        <template slot-scope="scope">
          <el-button-group>
            {{ scope.row.pushTime == null? "-":scope.row.pushTime }}
          </el-button-group>
        </template>
      </el-table-column>
      <el-table-column label="正在推流"  min-width="100">
        <template slot-scope="scope">
          {{scope.row.pushIng ? '是' : '否' }}
        </template>
      </el-table-column>
      <el-table-column label="本平台推流"  min-width="100">
        <template slot-scope="scope">
          {{scope.row.pushIng && !!scope.row.self ? '是' : '否' }}
        </template>
      </el-table-column>

      <el-table-column label="操作" min-width="360"  fixed="right">
        <template slot-scope="scope">
          <el-button size="medium" icon="el-icon-video-play"
                     v-if="scope.row.pushIng === true"
                     @click="playPush(scope.row)" type="text">播放
          </el-button>
          <el-divider direction="vertical"></el-divider>
          <el-button size="medium" icon="el-icon-delete" type="text" @click="stopPush(scope.row)" style="color: #f56c6c" >移除</el-button>
          <el-divider direction="vertical"></el-divider>
          <el-button size="medium" icon="el-icon-position" type="text" v-if="!!!scope.row.gbId"
                     @click="addToGB(scope.row)">加入国标
          </el-button>
          <el-divider v-if="!!!scope.row.gbId" direction="vertical"></el-divider>
          <el-button size="medium" icon="el-icon-position" type="text" v-if="!!scope.row.gbId"
                     @click="removeFromGB(scope.row)">移出国标
          </el-button>
          <el-button size="medium" icon="el-icon-cloudy" type="text" @click="queryCloudRecords(scope.row)">云端录像
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      style="float: right"
      @size-change="handleSizeChange"
      @current-change="currentChange"
      :current-page="currentPage"
      :page-size="count"
      :page-sizes="[15, 25, 35, 50]"
      layout="total, sizes, prev, pager, next"
      :total="total">
    </el-pagination>
    <streamProxyEdit ref="streamProxyEdit"></streamProxyEdit>
    <importChannel ref="importChannel"></importChannel>
  </div>
</template>

<script>
import streamProxyEdit from './dialog/StreamProxyEdit.vue'
import devicePlayer from './dialog/devicePlayer.vue'
import addStreamTOGB from './dialog/pushStreamEdit.vue'
import uiHeader from '../layout/UiHeader.vue'
import importChannel from './dialog/importChannel.vue'
import MediaServer from './service/MediaServer'

export default {
  name: 'pushVideoList',
  components: {
    devicePlayer,
    addStreamTOGB,
    streamProxyEdit,
    uiHeader,
    importChannel,
  },
  data() {
    return {
      pushList: [], //设备列表
      currentPusher: {}, //当前操作设备对象
      updateLooper: 0, //数据刷新轮训标志
      currentDeviceChannelsLenth: 0,
      winHeight: window.innerHeight - 250,
      mediaServerObj: new MediaServer(),
      currentPage: 1,
      count: 15,
      total: 0,
      searchSrt: "",
      pushing: "",
      mediaServerId: "",
      mediaServerList: [],
      multipleSelection: [],
      getDeviceListLoading: false
    };
  },
  computed: {},
  mounted() {
    this.initData();
    this.updateLooper = setInterval(this.getPushList, 2000);
  },
  destroyed() {
    clearTimeout(this.updateLooper);
  },
  methods: {
    initData: function () {
      this.mediaServerObj.getOnlineMediaServerList((data) => {
        this.mediaServerList = data.data;
      })
      this.getPushList();
    },
    currentChange: function (val) {
      this.currentPage = val;
      this.getPushList();
    },
    handleSizeChange: function (val) {
      this.count = val;
      this.getPushList();
    },
    getPushList: function () {
      let that = this;
      this.getDeviceListLoading = true;
      this.$axios({
        method: 'get',
        url: `/api/push/list`,
        params: {
          page: that.currentPage,
          count: that.count,
          query: that.searchSrt,
          pushing: that.pushing,
          mediaServerId: that.mediaServerId,
        }
      }).then(function (res) {
          if (res.data.code === 0) {
            that.total = res.data.data.total;
            that.pushList = res.data.data.list;
          }

        that.getDeviceListLoading = false;
      }).catch(function (error) {
        console.error(error);
        that.getDeviceListLoading = false;
      });
    },

    playPush: function (row) {
      let that = this;
      this.getListLoading = true;
      this.$axios({
        method: 'get',
        url: '/api/push/getPlayUrl',
        params: {
          app: row.app,
          stream: row.stream,
          mediaServerId: row.mediaServerId
        }
      }).then(function (res) {
        that.getListLoading = false;
        if (res.data.code === 0 ) {
          that.$refs.devicePlayer.openDialog("streamPlay", null, null, {
            streamInfo: res.data.data,
            hasAudio: true
          });
        }else {
          that.$message.error(res.data.msg);
        }

      }).catch(function (error) {
        console.error(error);
        that.getListLoading = false;
      });
    },
    stopPush: function (row) {
      let that = this;
      that.$axios({
        method: "post",
        url: "/api/push/stop",
        params: {
          app: row.app,
          streamId: row.stream
        }
      }).then((res) => {
        if (res.data.code === 0) {
          that.initData()
        }
      }).catch(function (error) {
        console.error(error);
      });
    },
    addToGB: function (row) {
      this.$refs.addStreamTOGB.openDialog({
        app: row.app,
        stream: row.stream,
        mediaServerId: row.mediaServerId
      }, this.initData);
    },
    removeFromGB: function (row) {
      let that = this;
      that.$axios({
        method: "delete",
        url: "/api/push/remove_form_gb",
        data: row
      }).then((res) => {
        if (res.data.code === 0) {
          that.initData()
        }
      }).catch(function (error) {
        console.error(error);
      });
    },
    queryCloudRecords: function (row) {

      this.$router.push(`/cloudRecordDetail/${row.app}/${row.stream}`)
    },
    importChannel: function () {
      this.$refs.importChannel.openDialog(() => {

      })
    },
    addStream: function (){
      this.$refs.addStreamTOGB.openDialog(null, this.initData);
    },
    batchDel: function () {
      this.$confirm(`确定删除选中的${this.multipleSelection.length}个通道?`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        let that = this;
        that.$axios({
          method: "delete",
          url: "/api/push/batchStop",
          data: {
            gbStreams: this.multipleSelection
          }
        }).then((res) => {
          this.initData();
          this.$refs.pushListTable.clearSelection();
        }).catch(function (error) {
          console.error(error);
        });
      }).catch(() => {

      });
    },
    handleSelectionChange: function (val) {
      this.multipleSelection = val;
    },
    refresh: function () {
      this.initData();
    },
  }
};
</script>

<style>
.videoList {
  display: flex;
  flex-wrap: wrap;
  align-content: flex-start;
}

.video-item {
  position: relative;
  width: 15rem;
  height: 10rem;
  margin-right: 1rem;
  background-color: #000000;
}

.video-item-img {
  position: absolute;
  top: 0;
  bottom: 0;
  left: 0;
  right: 0;
  margin: auto;
  width: 100%;
  height: 100%;
}

.video-item-img:after {
  content: "";
  display: inline-block;
  position: absolute;
  z-index: 2;
  top: 0;
  bottom: 0;
  left: 0;
  right: 0;
  margin: auto;
  width: 3rem;
  height: 3rem;
  background-image: url("../assets/loading.png");
  background-size: cover;
  background-color: #000000;
}

.video-item-title {
  position: absolute;
  bottom: 0;
  color: #000000;
  background-color: #ffffff;
  line-height: 1.5rem;
  padding: 0.3rem;
  width: 14.4rem;
}
</style>
