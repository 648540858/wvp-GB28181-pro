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
					<el-button icon="el-icon-arrow-left" size="mini" style="margin-right: 1rem;" @click="showDevice">返回</el-button>
					搜索: <el-input @input="search" style="margin-right: 1rem; width: auto;" size="mini" placeholder="关键字" prefix-icon="el-icon-search" v-model="searchSrt" clearable> </el-input>

					通道类型: <el-select size="mini" @change="search" style="margin-right: 1rem;" v-model="channelType" placeholder="请选择" default-first-option>
						<el-option label="全部" value="" ></el-option>
						<el-option label="设备" value="false"></el-option>
						<el-option label="子目录" value="true" ></el-option>
					</el-select>
					在线状态: <el-select size="mini" @change="search" v-model="online" placeholder="请选择" default-first-option>
						<el-option label="全部" value=""></el-option>
						<el-option label="在线" value="on"></el-option>
						<el-option label="离线" value="off"></el-option>
					</el-select>

				</div>
				<devicePlayer ref="devicePlayer"></devicePlayer>
				<!--设备列表-->
				<el-table ref="channelListTable" :data="deviceChannelList" :height="winHeight" border style="width: 100%">
					<el-table-column prop="channelId" label="通道编号" width="210">
						</el-table-column>
						<el-table-column prop="name" label="通道名称">
						</el-table-column>
						<el-table-column prop="subCount" label="子节点数">
						</el-table-column>
          <el-table-column label="开启音频" align="center">
            <template slot-scope="scope">
              <el-switch
                @change="updateChannel(scope.row)"
                v-model="scope.row.hasAudio"
                active-color="#409EFF">
              </el-switch>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="180" align="center">
            <template slot-scope="scope">
              <div slot="reference" class="name-wrapper">
                <el-tag size="medium" v-if="scope.row.status == 1">在线</el-tag>
                <el-tag size="medium" type="info" v-if="scope.row.status == 0">离线</el-tag>
              </div>
            </template>
          </el-table-column>
            <el-table-column prop="ptztypeText" label="云台类型">
            </el-table-column>
						<el-table-column label="操作" width="240" align="center" fixed="right">
							<template slot-scope="scope">
								<el-button size="mini" icon="el-icon-video-play" v-if="scope.row.parental == 0" @click="sendDevicePush(scope.row)">播放</el-button>
								<el-button size="mini" icon="el-icon-switch-button" type="danger" v-if="scope.row.play" @click="stopDevicePush(scope.row)">停止</el-button>
								<el-button size="mini" icon="el-icon-s-open"  type="primary" v-if="scope.row.parental == 1" @click="changeSubchannel(scope.row)">查看子目录</el-button>
								<!-- <el-button size="mini" @click="sendDevicePush(scope.row)">录像查询</el-button> -->
							</template>
						</el-table-column>
				</el-table>
				<el-pagination
					style="float: right"
					@size-change="handleSizeChange"
					@current-change="currentChange"
					:current-page="currentPage"
					:page-size="count"
					:page-sizes="[15, 20, 30, 50]"
					layout="total, sizes, prev, pager, next"
					:total="total">
				</el-pagination>

			</el-main>
		</el-container>
    <Loading v-if="isLoging" marginTop="-50%"></Loading>
	</div>
</template>

<script>
	 import devicePlayer from './gb28181/devicePlayer.vue'
	 import uiHeader from './UiHeader.vue'
   import Loading from './Loading.vue'
	export default {
		name: 'channelList',
		components: {
			devicePlayer,
			uiHeader,
      Loading
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
				total:0,
				beforeUrl:"/videoList",
        isLoging: false
			};
		},

		mounted() {
			this.initData();
			// this.updateLooper = setInterval(this.initData, 10000);
		},
		destroyed() {
			this.$destroy('videojs');
			clearTimeout(this.updateLooper);
		},
		methods: {
			initData: function() {
				if (this.parentChannelId == "" || this.parentChannelId == 0 ) {
					this.getDeviceChannelList();
				}else{
					this.showSubchannels();
				}

			},
			initParam: function(){
				this.deviceId= this.$route.params.deviceId;
				this.parentChannelId= this.$route.params.parentChannelId;
				this.currentPage= parseInt(this.$route.params.page);
				this.count= parseInt(this.$route.params.count);
				if (this.parentChannelId == "" || this.parentChannelId == 0 ) {
					this.beforeUrl = "/videoList"
				}

			},
			currentChange: function(val){
				var url = `/${this.$router.currentRoute.name}/${this.deviceId}/${this.parentChannelId}/${this.count}/${val}`
				console.log(url)
				this.$router.push(url).then(()=>{
					this.initParam();
					this.initData();
				})
			},
			handleSizeChange: function(val){
				var url = `/${this.$router.currentRoute.name}/${this.$router.params.deviceId}/${this.$router.params.parentChannelId}/${val}/1`
				this.$router.push(url).then(()=>{
					this.initParam();
					this.initData();
				})

			},
			getDeviceChannelList: function() {
				let that = this;
				console.log(this.currentPage - 1)

				this.$axios.get(`/api/devices/${this.$route.params.deviceId}/channels`,{
					params: {
						page: that.currentPage - 1,
						count: that.count,
						query: that.searchSrt,
						online: that.online,
						channelType: that.channelType
					}
				} )
				.then(function (res) {
					console.log(res);
					that.total = res.data.total;
					that.deviceChannelList = res.data.data;
					// 防止出现表格错位
					that.$nextTick(()=>{
						that.$refs.channelListTable.doLayout();
					})
				})
				.catch(function (error) {
					console.log(error);
				});

			},


			//gb28181平台对接
			//刷新设备信息
			refDevice: function(itemData) {
				///api/devices/{deviceId}/sync
				console.log("刷新对应设备:" + itemData.deviceId);
				this.$axios({
					method: 'post',
					url: '/api/devices/' + itemData.deviceId + '/sync'
				}).then(function(res) {
					// console.log("刷新设备结果："+JSON.stringify(res));
				}).catch(function(e) {
					that.$message({
						showClose: true,
						message: '请求成功',
						type: 'success'
					});
				});
			},
			//通知设备上传媒体流
			sendDevicePush: function(itemData) {
			  console.log(itemData)
				let deviceId = this.deviceId;
        this.isLoging = true;
				let channelId = itemData.channelId;
				console.log("通知设备推流1：" + deviceId + " : " + channelId);
				let that = this;
				this.$axios({
					method: 'get',
					url: '/api/play/' + deviceId + '/' + channelId
				}).then(function(res) {
          console.log(res.data)
					let ssrc = res.data.ssrc;
          that.isLoging = false
          if (!!ssrc) {
            that.$refs.devicePlayer.play(res.data,deviceId,channelId,itemData.hasAudio);
            that.initData();
          }else {
            that.$message.error(res.data);
          }
				}).catch(function(e) {
				});
			},
      stopDevicePush: function(itemData) {
			  console.log(itemData)
        var that = this;
        this.$axios({
          method: 'post',
          url: '/api/play/' + itemData.ssrc + '/stop'
        }).then(function(res) {
          console.log(JSON.stringify(res));
          that.initData();
        });
      },

			showDevice: function(){
				this.$router.push(this.beforeUrl).then(()=>{
					this.initParam();
					this.initData();
				})
			},
			changeSubchannel(itemData) {
				console.log(this.$router.currentRoute)
				this.beforeUrl = this.$router.currentRoute.path;

				var url = `/${this.$router.currentRoute.name}/${this.$router.currentRoute.params.deviceId}/${itemData.channelId}/${this.$router.currentRoute.params.count}/1`
				this.$router.push(url).then(()=>{
					this.searchSrt= "";
					this.channelType= "";
					this.online= "";
					this.initParam();
					this.initData();
				})
			},
			showSubchannels: function(channelId){
				let that = this;

				this.$axios.get(`/api/subChannels/${this.deviceId}/${this.parentChannelId}/channels`,{
					params: {
						page: that.currentPage - 1,
						count: that.count,
						query: that.searchSrt,
						online: that.online,
						channelType: that.channelType
					}
				} )
				.then(function (res) {
					that.total = res.data.total;
					that.deviceChannelList = res.data.data;
					// 防止出现表格错位
					that.$nextTick(()=>{
						that.$refs.channelListTable.doLayout();
					})
				})
				.catch(function (error) {
					console.log(error);
				});
			},
			search: function() {
				console.log(this.searchSrt)
				this.currentPage = 1;
				this.total = 0;
				this.initData();
			},
      updateChannel: function(row) {
				console.log(row)
        this.$axios({
          method: 'post',
          url: `/api/channel/update/${this.deviceId}`,
          params: row
        }).then(function(res) {
          console.log(JSON.stringify(res));
        });
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
