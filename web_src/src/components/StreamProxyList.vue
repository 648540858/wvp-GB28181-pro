<template>
	<div id="streamProxyList" style="width: 100%">
    <div class="page-header">
      <div class="page-title">拉流代理列表</div>
      <div class="page-header-btn">
        <el-button icon="el-icon-plus" size="mini" style="margin-right: 1rem;" type="primary" @click="addStreamProxy">添加代理</el-button>
        <el-button v-if="false" icon="el-icon-search" size="mini" style="margin-right: 1rem;" type="primary" @click="addOnvif">搜索ONVIF</el-button>
        <el-button icon="el-icon-refresh-right" circle size="mini" @click="refresh()"></el-button>
      </div>
    </div>
    <devicePlayer ref="devicePlayer"></devicePlayer>
    <el-table :data="streamProxyList" style="width: 100%" :height="winHeight">
      <el-table-column prop="name" label="名称" min-width="120" show-overflow-tooltip/>
      <el-table-column prop="app" label="流应用名" min-width="120" show-overflow-tooltip/>
      <el-table-column prop="stream" label="流ID" min-width="120" show-overflow-tooltip/>
      <el-table-column label="流地址" min-width="400"  show-overflow-tooltip >
        <template slot-scope="scope">
          <div slot="reference" class="name-wrapper">

            <el-tag size="medium" v-if="scope.row.type == 'default'">
              <i class="cpoy-btn el-icon-document-copy"  title="点击拷贝" v-clipboard="scope.row.url" @success="$message({type:'success', message:'成功拷贝到粘贴板'})"></i>
              {{scope.row.url}}
            </el-tag>
            <el-tag size="medium" v-if="scope.row.type != 'default'">
              <i class="cpoy-btn el-icon-document-copy"  title="点击拷贝" v-clipboard="scope.row.srcUrl" @success="$message({type:'success', message:'成功拷贝到粘贴板'})"></i>
              {{scope.row.srcUrl}}
            </el-tag>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="mediaServerId" label="流媒体" min-width="180" ></el-table-column>
      <el-table-column label="类型" width="100" >
        <template slot-scope="scope">
          <div slot="reference" class="name-wrapper">
            <el-tag size="medium">{{scope.row.type === "default"? "直接代理":"FFMPEG代理"}}</el-tag>
          </div>
        </template>
      </el-table-column>

      <el-table-column prop="gbId" label="国标编码" min-width="180"  show-overflow-tooltip/>
      <el-table-column label="状态" min-width="120" >
        <template slot-scope="scope">
          <div slot="reference" class="name-wrapper">
            <el-tag size="medium" v-if="scope.row.status">在线</el-tag>
            <el-tag size="medium" type="info" v-if="!scope.row.status">离线</el-tag>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="启用" min-width="120" >
        <template slot-scope="scope">
          <div slot="reference" class="name-wrapper">
            <el-tag size="medium" v-if="scope.row.enable">已启用</el-tag>
            <el-tag size="medium" type="info" v-if="!scope.row.enable">未启用</el-tag>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间"  min-width="150" show-overflow-tooltip/>
      <el-table-column label="音频" min-width="120" >
        <template slot-scope="scope">
          <div slot="reference" class="name-wrapper">
            <el-tag size="medium" v-if="scope.row.enableAudio">已启用</el-tag>
            <el-tag size="medium" type="info" v-if="!scope.row.enableAudio">未启用</el-tag>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="录制" min-width="120" >
        <template slot-scope="scope">
          <div slot="reference" class="name-wrapper">
            <el-tag size="medium" v-if="scope.row.enableMp4">已启用</el-tag>
            <el-tag size="medium" type="info" v-if="!scope.row.enableMp4">未启用</el-tag>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="无人观看" min-width="160" >
        <template slot-scope="scope">
          <div slot="reference" class="name-wrapper">
            <el-tag size="medium" v-if="scope.row.enableRemoveNoneReader">移除</el-tag>
            <el-tag size="medium" v-if="scope.row.enableDisableNoneReader">停用</el-tag>
            <el-tag size="medium" type="info" v-if="!scope.row.enableRemoveNoneReader && !scope.row.enableDisableNoneReader">不做处理</el-tag>
          </div>
        </template>
      </el-table-column>


      <el-table-column label="操作" width="360"  fixed="right">
        <template slot-scope="scope">
          <el-button size="medium" icon="el-icon-video-play" type="text" v-if="scope.row.enable" @click="play(scope.row)">播放</el-button>
          <el-divider direction="vertical"></el-divider>
          <el-button size="medium" icon="el-icon-switch-button" type="text" v-if="scope.row.enable" @click="stop(scope.row)">停用</el-button>
          <el-divider direction="vertical"></el-divider>
          <el-button size="medium" icon="el-icon-check" type="text" :loading="scope.row.startBtnLoading" v-if="!scope.row.enable" @click="start(scope.row)">启用</el-button>
          <el-divider v-if="!scope.row.enable" direction="vertical"></el-divider>
          <el-button size="medium" icon="el-icon-delete" type="text" style="color: #f56c6c" @click="deleteStreamProxy(scope.row)">删除</el-button>
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
    <streamProxyEdit ref="streamProxyEdit" ></streamProxyEdit>
    <onvifEdit ref="onvifEdit" ></onvifEdit>
	</div>
</template>

<script>
	import streamProxyEdit from './dialog/StreamProxyEdit.vue'
	import onvifEdit from './dialog/onvifEdit.vue'
	import devicePlayer from './dialog/devicePlayer.vue'
	import uiHeader from '../layout/UiHeader.vue'
	export default {
		name: 'streamProxyList',
		components: {
			devicePlayer,
			streamProxyEdit,
      onvifEdit,
			uiHeader
		},
		data() {
			return {
				streamProxyList: [],
				currentPusher: {}, //当前操作设备对象
				updateLooper: 0, //数据刷新轮训标志
				currentDeviceChannelsLenth:0,
				winHeight: window.innerHeight - 250,
				currentPage:1,
				count:15,
				total:0,
        startBtnLoading: false
			};
		},
		computed: {
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
			},
      stopUpdateList: function (){
        window.clearInterval(this.updateLooper)
      },
      startUpdateList: function (){
        this.updateLooper = setInterval(this.initData, 1000);
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
						count: that.count
					}
				}).then(function (res) {
          if (res.data.code === 0) {
            that.total = res.data.data.total;
            for (let i = 0; i < res.data.data.list.length; i++) {
              res.data.data.list[i]["startBtnLoading"] = false;
            }
            that.streamProxyList = res.data.data.list;
          }
				}).catch(function (error) {
					console.log(error);
				});
			},
			addStreamProxy: function(){
				this.$refs.streamProxyEdit.openDialog(null, this.initData)
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
              this.$message.success("未找到可用设备");
            }
        }else {
            this.$message.error(res.data.msg);
          }

        }).catch((error)=> {
          this.$message.error(error.response.data.msg);
        });

			},
			saveStreamProxy: function(){
			},
			play: function(row){
				let that = this;
				this.$axios({
					method: 'get',
					url:`/api/push/getPlayUrl`,
					params: {
						app: row.app,
						stream: row.stream,
            mediaServerId: row.mediaServerId
					}
				}).then(function (res) {
					if (res.data.code === 0) {
            that.$refs.devicePlayer.openDialog("streamPlay", null, null, {
              streamInfo: res.data.data,
              hasAudio: true
            });
          }else {
            that.$message({
              showClose: true,
              message: "获取地址失败：" + res.data.msg,
              type: "error",
            });
          }

				}).catch(function (error) {
					console.log(error);
				});

			},
      queryCloudRecords: function (row) {

        this.$router.push(`/cloudRecordDetail/${row.app}/${row.stream}`)
      },
			deleteStreamProxy: function(row){
				let that = this;
        this.$confirm('确定删除此代理吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }).then(() => {
          that.$axios({
            method:"delete",
            url:"/api/proxy/del",
            params:{
              app: row.app,
              stream: row.stream
            }
          }).then((res)=>{
            that.initData()
          }).catch(function (error) {
            console.log(error);
          });
        }).catch(() => {
        });
			},
			start: function(row){
        this.stopUpdateList()
        this.$set(row, 'startBtnLoading', true)
				this.$axios({
					method: 'get',
					url:`/api/proxy/start`,
					params: {
						app: row.app,
						stream: row.stream
					}
				}).then((res)=> {
				  if (res.data.code === 0){
            this.initData()
          }else {
            this.$message({
              showClose: true,
              message: "启动失败，请检查地址是否可用！",
              type: "error",
            });
          }
          this.$set(row, 'startBtnLoading', false)
          this.startUpdateList()
				}).catch((error)=> {
					console.log(error);
          this.$message({
            showClose: true,
            message: "启动失败，请检查地址是否可用！",
            type: "error",
          });
          this.$set(row, 'startBtnLoading', false)
          this.startUpdateList()
				});
			},
			stop: function(row){
				let that = this;
				this.$axios({
					method: 'get',
					url:`/api/proxy/stop`,
					params: {
						app: row.app,
						stream: row.stream
					}
				}).then(function (res) {
					that.initData()
				}).catch(function (error) {
					console.log(error);
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
