<template>
	<div id="streamProxyList" style="width: 100%">

    <div v-if="!streamProxy">
      <div class="page-header">
        <div class="page-title">拉流代理列表</div>
        <div class="page-header-btn">
          搜索:
          <el-input @input="getStreamProxyList" style="margin-right: 1rem; width: auto;" size="mini" placeholder="关键字"
                    prefix-icon="el-icon-search" v-model="searchSrt" clearable></el-input>
          流媒体:
          <el-select size="mini" @change="getStreamProxyList" style="margin-right: 1rem;" v-model="mediaServerId"
                     placeholder="请选择" default-first-option>
            <el-option label="全部" value=""></el-option>
            <el-option
              v-for="item in mediaServerList"
              :key="item.id"
              :label="item.id"
              :value="item.id">
            </el-option>
          </el-select>
          拉流状态:
          <el-select size="mini" style="margin-right: 1rem;" @change="getStreamProxyList" v-model="pulling" placeholder="请选择"
                     default-first-option>
            <el-option label="全部" value=""></el-option>
            <el-option label="正在拉流" value="true"></el-option>
            <el-option label="尚未拉流" value="false"></el-option>
          </el-select>
          <el-button icon="el-icon-plus" size="mini" style="margin-right: 1rem;" type="primary" @click="addStreamProxy">添加代理</el-button>
          <el-button v-if="false" icon="el-icon-search" size="mini" style="margin-right: 1rem;" type="primary" @click="addOnvif">搜索ONVIF</el-button>
          <el-button icon="el-icon-refresh-right" circle size="mini" @click="refresh()"></el-button>
        </div>
      </div>
      <devicePlayer ref="devicePlayer"></devicePlayer>
      <el-table size="medium"  :data="streamProxyList" style="width: 100%" :height="$tableHeght" >
        <el-table-column prop="app" label="流应用名" min-width="120" show-overflow-tooltip/>
        <el-table-column prop="stream" label="流ID" min-width="120" show-overflow-tooltip/>
        <el-table-column label="流地址" min-width="250"  show-overflow-tooltip >
          <template v-slot:default="scope">
            <div slot="reference" class="name-wrapper">
              <el-tag size="medium">
                <i class="cpoy-btn el-icon-document-copy"  title="点击拷贝" v-clipboard="scope.row.srcUrl" @success="$message({type:'success', message:'成功拷贝到粘贴板'})"></i>
                {{scope.row.srcUrl}}
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="mediaServerId" label="流媒体" min-width="180" ></el-table-column>
        <el-table-column label="代理方式" width="100" >
          <template v-slot:default="scope">
            <div slot="reference" class="name-wrapper">
              {{scope.row.type === "default"? "默认":"FFMPEG代理"}}
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="gbDeviceId" label="国标编码" min-width="180"  show-overflow-tooltip/>
        <el-table-column label="拉流状态" min-width="120" >
          <template v-slot:default="scope">
            <div slot="reference" class="name-wrapper">
              <el-tag size="medium" v-if="scope.row.pulling && Vue.prototype.$myServerId !== scope.row.serverId" style="border-color: #ecf1af">正在拉流</el-tag>
              <el-tag size="medium" v-if="scope.row.pulling && Vue.prototype.$myServerId === scope.row.serverId">正在拉流</el-tag>
              <el-tag size="medium" type="info" v-if="!scope.row.pulling">尚未拉流</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="启用" min-width="120" >
          <template v-slot:default="scope">
            <div slot="reference" class="name-wrapper">
              <el-tag size="medium" v-if="scope.row.enable && Vue.prototype.$myServerId !== scope.row.serverId" style="border-color: #ecf1af">已启用</el-tag>
              <el-tag size="medium" v-if="scope.row.enable && Vue.prototype.$myServerId === scope.row.serverId">已启用</el-tag>
              <el-tag size="medium" type="info" v-if="!scope.row.enable">未启用</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间"  min-width="150" show-overflow-tooltip/>
        <el-table-column label="操作" width="400"  fixed="right">
          <template v-slot:default="scope">
            <el-button size="medium" :loading="scope.row.playLoading" icon="el-icon-video-play" type="text" @click="play(scope.row)">播放</el-button>
            <el-divider direction="vertical"></el-divider>
            <el-button size="medium" icon="el-icon-switch-button" style="color: #f56c6c"  type="text" v-if="scope.row.pulling" @click="stopPlay(scope.row)">停止</el-button>
            <el-divider direction="vertical" v-if="scope.row.pulling" ></el-divider>
            <el-button size="medium" icon="el-icon-edit" type="text" @click="edit(scope.row)">
              编辑
            </el-button>
            <el-divider direction="vertical"></el-divider>
            <el-button size="medium" icon="el-icon-cloudy" type="text" @click="queryCloudRecords(scope.row)">云端录像</el-button>
            <el-divider direction="vertical"></el-divider>
            <el-button size="medium" icon="el-icon-delete" type="text" style="color: #f56c6c" @click="deleteStreamProxy(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        style="text-align: right"
        @size-change="handleSizeChange"
        @current-change="currentChange"
        :current-page="currentPage"
        :page-size="count"
        :page-sizes="[15, 25, 35, 50]"
        layout="total, sizes, prev, pager, next"
        :total="total">
      </el-pagination>
    </div>
    <streamProxyEdit ref="streamProxyEdit" ></streamProxyEdit>
    <onvifEdit ref="onvifEdit" ></onvifEdit>
    <StreamProxyEdit v-if="streamProxy" v-model="streamProxy" :closeEdit="closeEdit" ></StreamProxyEdit>
	</div>
</template>

<script>
	import streamProxyEdit from './dialog/StreamProxyEdit.vue'
	import onvifEdit from './dialog/onvifEdit.vue'
	import devicePlayer from './dialog/devicePlayer.vue'
	import uiHeader from '../layout/UiHeader.vue'
  import StreamProxyEdit from "./StreamProxyEdit";
  import MediaServer from "./service/MediaServer";
  import Vue from "vue";

	export default {
		name: 'streamProxyList',
		components: {
			devicePlayer,
			streamProxyEdit,
      onvifEdit,
      StreamProxyEdit,
			uiHeader
		},
		data() {
			return {
				streamProxyList: [],
				currentPusher: {}, //当前操作设备对象
				updateLooper: 0, //数据刷新轮训标志
				currentDeviceChannelsLenth:0,
				currentPage:1,
				count:15,
				total:0,
        streamProxy: null,
        searchSrt: "",
        mediaServerId: "",
        pulling: "",
        mediaServerObj: new MediaServer(),
        mediaServerList: [],
			};
		},
		computed: {
      Vue() {
        return Vue
      },
		},
		mounted() {
			this.initData();
			this.startUpdateList()
		},
		destroyed() {
			this.$destroy('videojs');
			clearTimeout(this.updateLooper);
		},
		methods: {
			initData: function() {
				this.getStreamProxyList();
        this.mediaServerObj.getOnlineMediaServerList((data) => {
          this.mediaServerList = data.data;
        })
			},
      startUpdateList: function (){
        this.updateLooper = setInterval(()=>{
          if (!this.streamProxy) {
            this.getStreamProxyList()
          }

        }, 1000);
      },
			currentChange: function(val){
				this.currentPage = val;
				this.getStreamProxyList();
			},
			handleSizeChange: function(val){
				this.count = val;
				this.getStreamProxyList();
			},
			getStreamProxyList: function() {
				let that = this;
				this.$axios({
					method: 'get',
					url:`/api/proxy/list`,
					params: {
						page: that.currentPage,
						count: that.count,
            query: this.searchSrt,
            pulling: this.pulling,
            mediaServerId: this.mediaServerId,
					}
				}).then(function (res) {
          if (res.data.code === 0) {
            that.total = res.data.data.total;
            for (let i = 0; i < res.data.data.list.length; i++) {
              res.data.data.list[i]["playLoading"] = false;
            }
            that.streamProxyList = res.data.data.list;
          }
				}).catch(function (error) {
					console.log(error);
				});
			},
			addStreamProxy: function(){
				// this.$refs.streamProxyEdit.openDialog(null, this.initData)
        this.streamProxy = {
          type: "default",
          dataType: 3,
          noneReader: 1,
          enable: true,
          enableAudio: true,
          mediaServerId: "",
          timeout: 10,
        }
			},
      addOnvif: function(){
        this.$axios({
          method: 'get',
          url:`/api/onvif/search?timeout=3000`,
        }).then((res) =>{
          if (res.data.code === 0 ){
            if (res.data.data.length > 0) {
              this.$refs.onvifEdit.openDialog(res.data.data, (url)=>{
                  if (url != null) {
                    this.$refs.onvifEdit.close();
                    this.$refs.streamProxyEdit.openDialog({type: "default", url: url, srcUrl: url}, this.initData())
                  }
              })
            }else {
              this.$message.success({
                showClose: true,
                message: "未找到可用设备"
              });
            }
        }else {
            this.$message.error({
              showClose: true,
              message: res.data.msg
            })
          }

        }).catch((error)=> {
          this.$message.error({
            showClose: true,
            message: error
          })
        });

			},
      edit: function(row){
        if (row.enableDisableNoneReader) {
          this.$set(row, "noneReader", 1)
        }else if (row.enableRemoveNoneReader) {
          this.$set(row, "noneReader", 2)
        }else {
          this.$set(row, "noneReader", 0)
        }
        this.streamProxy = row
        this.$set(this.streamProxy, "rtspType", row.rtspType)
			},
      closeEdit: function(row){
        this.streamProxy = null
			},
			play: function(row){
        row.playLoading = true;
				this.$axios({
					method: 'get',
					url:`/api/proxy/start`,
					params: {
						id: row.id,
					}
				}).then((res)=> {
					if (res.data.code === 0) {
            this.$refs.devicePlayer.openDialog("streamPlay", null, null, {
              streamInfo: res.data.data,
              hasAudio: true
            });
          }else {
            this.$message({
              showClose: true,
              message: "获取地址失败：" + res.data.msg,
              type: "error",
            });
          }

				}).catch(function (error) {
					console.log(error);
				}).finally(()=>{
          row.playLoading = false;
        })

			},
      stopPlay: function(row){
				let that = this;
				this.$axios({
					method: 'get',
					url:`/api/proxy/stop`,
					params: {
						id: row.id,
					}
				}).then(function (res) {
					if (res.data.code === 0) {

          }else {
            that.$message.error(res.data.msg);
          }

				}).catch(function (error) {
					console.log(error);
				});

			},
      queryCloudRecords: function (row) {

        this.$router.push(`/cloudRecordDetail/${row.app}/${row.stream}`)
      },
			deleteStreamProxy: function(row){
        this.$confirm('确定删除此代理吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }).then(() => {
          this.$axios({
            method:"delete",
            url:"/api/proxy/delete",
            params:{
              id: row.id,
            }
          }).then((res)=>{
            this.$message.success({
              showClose: true,
              message: "删除成功"
            })
            this.initData()
          }).catch((error) =>{
            console.log(error);
          });
        }).catch(() => {
        });
			},
      refresh: function (){
        this.initData();
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
	.cpoy-btn {
		cursor: pointer;
		margin-right: 10px;
	}
</style>
