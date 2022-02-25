<template>
<div id="channelList">
    <el-container>
        <el-header>
            <uiHeader></uiHeader>
        </el-header>
        <el-main>
            <div style="background-color: #FFFFFF; position: relative; padding: 1rem 0.5rem 0.5rem 0.5rem; text-align: center;">
                <span style="font-size: 1rem; font-weight: 500; ">通道列表({{parentChannelId ==0 ? deviceId:parentChannelId}})</span>

            </div>
            <div style="background-color: #FFFFFF; margin-bottom: 1rem; position: relative; padding: 0.5rem; text-align: left;font-size: 14px;">
                <el-button icon="el-icon-arrow-left" size="mini" style="margin-right: 1rem;" type="primary" @click="showDevice">返回</el-button>
                搜索: <el-input @input="search" style="margin-right: 1rem; width: auto;" size="mini" placeholder="关键字" prefix-icon="el-icon-search" v-model="searchSrt" clearable> </el-input>

                通道类型: <el-select size="mini" @change="search" style="margin-right: 1rem;" v-model="channelType" placeholder="请选择" default-first-option>
                    <el-option label="全部" value=""></el-option>
                    <el-option label="设备" value="false"></el-option>
                    <el-option label="子目录" value="true"></el-option>
                </el-select>
                在线状态: <el-select size="mini" style="margin-right: 1rem;" @change="search" v-model="online" placeholder="请选择" default-first-option>
                    <el-option label="全部" value=""></el-option>
                    <el-option label="在线" value="true"></el-option>
                    <el-option label="离线" value="false"></el-option>
                </el-select>
                <el-checkbox size="mini" style="margin-right: 1rem; float: right;" v-model="autoList" @change="autoListChange">自动刷新</el-checkbox>
            </div>
            <devicePlayer ref="devicePlayer" v-loading="isLoging"></devicePlayer>
            <!--设备列表-->
            <el-table ref="channelListTable" :data="deviceChannelList" :height="winHeight" border style="width: 100%">
                <el-table-column prop="channelId" label="通道编号" width="200">
                </el-table-column>
                <el-table-column prop="name" label="通道名称">
                </el-table-column>
                <el-table-column label="快照" width="80" align="center">
                  <template slot-scope="scope">
                    <img style="max-height: 3rem;max-width: 4rem;"
                         :id="scope.row.deviceId + '_' + scope.row.channelId"
                         :src="getSnap(scope.row)"
                         @error="getSnapErrorEvent($event.target.id)"
                         alt="">
<!--                    <el-image-->
<!--                      :id="'snapImg_' + scope.row.deviceId + '_' + scope.row.channelId"-->
<!--                      :src="getSnap(scope.row)"-->
<!--                      @error="getSnapErrorEvent($event, scope.row)"-->
<!--                      :fit="'contain'">-->
<!--                      <div slot="error" class="image-slot">-->
<!--                        <i class="el-icon-picture-outline"></i>-->
<!--                      </div>-->
<!--                    </el-image>-->
                  </template>
                </el-table-column>
                <el-table-column prop="subCount" label="子节点数">
                </el-table-column>
                <el-table-column prop="manufacture" label="厂家">
                </el-table-column>
                <el-table-column label="位置信息" align="center">
                  <template slot-scope="scope">
                    <span>{{scope.row.longitude}},{{scope.row.latitude}}</span>
                  </template>
                </el-table-column>
              <el-table-column prop="ptztypeText" label="云台类型"/>
                <el-table-column label="开启音频" align="center">
                    <template slot-scope="scope">
                        <el-switch @change="updateChannel(scope.row)" v-model="scope.row.hasAudio" active-color="#409EFF">
                        </el-switch>
                    </template>
                </el-table-column>
                <el-table-column label="状态" width="180" align="center">
                    <template slot-scope="scope">
                        <div slot="reference" class="name-wrapper">
                            <el-tag size="medium" v-if="scope.row.status == 1">开启</el-tag>
                            <el-tag size="medium" type="info" v-if="scope.row.status == 0">关闭</el-tag>
                        </div>
                    </template>
                </el-table-column>


                <el-table-column label="操作" width="280" align="center" fixed="right">
                    <template slot-scope="scope">
                        <el-button-group>
                            <!-- <el-button size="mini" icon="el-icon-video-play" v-if="scope.row.parental == 0" @click="sendDevicePush(scope.row)">播放</el-button> -->
                            <el-button size="mini" icon="el-icon-video-play" @click="sendDevicePush(scope.row)">播放</el-button>
                            <el-button size="mini" icon="el-icon-switch-button" type="danger" v-if="!!scope.row.streamId" @click="stopDevicePush(scope.row)">停止</el-button>
                            <el-button size="mini" icon="el-icon-s-open" type="primary" v-if="scope.row.subCount > 0" @click="changeSubchannel(scope.row)">查看</el-button>
                            <el-button size="mini" icon="el-icon-video-camera" type="primary" @click="queryRecords(scope.row)">设备录象</el-button>
                            <!--                             <el-button size="mini" @click="sendDevicePush(scope.row)">录像查询</el-button> -->
                        </el-button-group>
                    </template>
                </el-table-column>
            </el-table>
            <el-pagination style="float: right" @size-change="handleSizeChange" @current-change="currentChange" :current-page="currentPage" :page-size="count" :page-sizes="[15, 20, 30, 50]" layout="total, sizes, prev, pager, next" :total="total">
            </el-pagination>

        </el-main>
    </el-container>
</div>
</template>

<script>
import devicePlayer from './dialog/devicePlayer.vue'
import uiHeader from './UiHeader.vue'
import moment from "moment";
export default {
    name: 'channelList',
    components: {
        devicePlayer,
        uiHeader
    },
    data() {
        return {
            deviceId: this.$route.params.deviceId,
            parentChannelId: this.$route.params.parentChannelId,
            deviceChannelList: [],
            videoComponentList: [],
            currentPlayerInfo: {}, //当前播放对象
            updateLooper: 0, //数据刷新轮训标志
            searchSrt: "",
            channelType: "",
            online: "",
            winHeight: window.innerHeight - 250,
            currentPage: parseInt(this.$route.params.page),
            count: parseInt(this.$route.params.count),
            total: 0,
            beforeUrl: "/deviceList",
            isLoging: false,
            autoList: true,
            loadSnap:{}
        };
    },

    mounted() {
        this.initData();
        if (this.autoList) {
            this.updateLooper = setInterval(this.initData, 5000);
        }

    },
    destroyed() {
        this.$destroy('videojs');
        clearTimeout(this.updateLooper);
    },
    methods: {
        initData: function () {
            if (typeof (this.parentChannelId) == "undefined" ||  this.parentChannelId == 0) {
                this.getDeviceChannelList();
            } else {
                this.showSubchannels();
            }
        },
        initParam: function () {
            this.deviceId = this.$route.params.deviceId;
            this.parentChannelId = this.$route.params.parentChannelId;
            this.currentPage = parseInt(this.$route.params.page);
            this.count = parseInt(this.$route.params.count);
            if (this.parentChannelId == "" || this.parentChannelId == 0) {
                this.beforeUrl = "/deviceList"
            }

        },
        currentChange: function (val) {
            var url = `/${this.$router.currentRoute.name}/${this.deviceId}/${this.parentChannelId}/${this.count}/${val}`
            this.$router.push(url).then(() => {
                this.initParam();
                this.initData();
            })
        },
        handleSizeChange: function (val) {
            var url = `/${this.$router.currentRoute.name}/${this.$router.params.deviceId}/${this.$router.params.parentChannelId}/${val}/1`
            this.$router.push(url).then(() => {
                this.initParam();
                this.initData();
            })

        },
        getDeviceChannelList: function () {
            let that = this;
            if (typeof (this.$route.params.deviceId) == "undefined") return;
            this.$axios({
                method: 'get',
                url: `/api/device/query/devices/${this.$route.params.deviceId}/channels`,
                params:{
						            page: that.currentPage,
                        count: that.count,
                        query: that.searchSrt,
                        online: that.online,
                        channelType: that.channelType
					      }
            }).then(function (res) {
                    that.total = res.data.total;
                    that.deviceChannelList = res.data.list;
                    // 防止出现表格错位
                    that.$nextTick(() => {
                        that.$refs.channelListTable.doLayout();
                    })
            }).catch(function (error) {
                console.log(error);
            });
        },

        //通知设备上传媒体流
        sendDevicePush: function (itemData) {
            let deviceId = this.deviceId;
            this.isLoging = true;
            let channelId = itemData.channelId;
            console.log("通知设备推流1：" + deviceId + " : " + channelId );
            let that = this;
            this.$axios({
                method: 'get',
                url: '/api/play/start/' + deviceId + '/' + channelId
            }).then(function (res) {
                that.isLoging = false;
                if (res.data.code === 0) {

                  setTimeout(()=>{

                    let snapId = deviceId + "_" + channelId;
                    that.loadSnap[snapId] = 0;
                    that.getSnapErrorEvent(snapId)
                  },5000)
                  that.$refs.devicePlayer.openDialog("media", deviceId, channelId, {
                    streamInfo: res.data.data,
                    hasAudio: itemData.hasAudio
                  });
                  setTimeout(()=>{
                    that.initData();
                  },1000)

                }else {
                  that.$message.error(res.data.msg);
                }
            }).catch(function (e) {});
        },
        queryRecords: function (itemData) {
            var format = moment().format("YYYY-M-D");
            let deviceId = this.deviceId;
            let channelId = itemData.channelId;
            this.$refs.devicePlayer.openDialog("record", deviceId, channelId, {date: format})
        },
        stopDevicePush: function (itemData) {
            var that = this;
            this.$axios({
                method: 'get',
                url: '/api/play/stop/' + this.deviceId + "/" + itemData.channelId
            }).then(function (res) {
                that.initData();
            }).catch(function (error) {
              if (error.response.status === 402) { // 已经停止过
                that.initData();
              }else {
                console.log(error)
              }
            });
        },
        getSnap: function (row){
          return '/static/snap/' + row.deviceId + '_' + row.channelId + '.jpg'
        },
        getSnapErrorEvent: function (id){

          if (typeof (this.loadSnap[id]) != "undefined") {
            console.log("下载截图" + this.loadSnap[id])
            if (this.loadSnap[id] > 5) {
              delete this.loadSnap[id];
              return;
            }
            setTimeout(()=>{
              this.loadSnap[id] ++
              document.getElementById(id).setAttribute("src", '/static/snap/' + id + '.jpg?' + new Date().getTime())
            },1000)

          }
        },
        showDevice: function () {
            this.$router.push(this.beforeUrl).then(() => {
                this.initParam();
                this.initData();
            })
        },
        changeSubchannel(itemData) {
            this.beforeUrl = this.$router.currentRoute.path;

            var url = `/${this.$router.currentRoute.name}/${this.$router.currentRoute.params.deviceId}/${itemData.channelId}/${this.$router.currentRoute.params.count}/1`
            this.$router.push(url).then(() => {
                this.searchSrt = "";
                this.channelType = "";
                this.online = "";
                this.initParam();
                this.initData();
            })
        },
        showSubchannels: function (channelId) {
            let that = this;

            this.$axios({
                method: 'get',
                url:`/api/device/query/sub_channels/${this.deviceId}/${this.parentChannelId}/channels`,
                params: {
                    page: that.currentPage,
                    count: that.count,
                    query: that.searchSrt,
                    online: that.online,
                    channelType: that.channelType
                }
            }).then(function (res) {
                that.total = res.data.total;
                that.deviceChannelList = res.data.list;
                // 防止出现表格错位
                that.$nextTick(() => {
                    that.$refs.channelListTable.doLayout();
                })
            }).catch(function (error) {
                console.log(error);
            });
        },
        search: function () {
            this.currentPage = 1;
            this.total = 0;
            this.initData();
        },
        updateChannel: function (row) {
            this.$axios({
                method: 'post',
                url: `/api/device/query/channel/update/${this.deviceId}`,
                params: row
            }).then(function (res) {
                console.log(JSON.stringify(res));
            });
        },
        autoListChange: function () {
            if (this.autoList) {
                this.updateLooper = setInterval(this.initData, 1500);
            }else{
                window.clearInterval(this.updateLooper);
            }
        }

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
