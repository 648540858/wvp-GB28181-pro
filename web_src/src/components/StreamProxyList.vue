<template>
	<div id="streamProxyList">
		<el-container>
			<el-header>
				<uiHeader></uiHeader>
			</el-header>
			<el-main>
				<div style="background-color: #FFFFFF; margin-bottom: 1rem; position: relative; padding: 0.5rem; text-align: left;">
					<span style="font-size: 1rem; font-weight: bold;">拉流代理列表</span>
				</div>
				<div style="background-color: #FFFFFF; margin-bottom: 1rem; position: relative; padding: 0.5rem; text-align: left;font-size: 14px;">
					<el-button icon="el-icon-plus" size="mini" style="margin-right: 1rem;" type="primary" @click="addStreamProxy">添加代理</el-button>
				</div>
				<devicePlayer ref="devicePlayer"></devicePlayer>
				<el-table :data="streamProxyList" border style="width: 100%" :height="winHeight">
					<el-table-column prop="name" label="名称" align="center" show-overflow-tooltip/>
					<el-table-column prop="app" label="流应用名" align="center" show-overflow-tooltip/>
					<el-table-column prop="stream" label="流ID" align="center" show-overflow-tooltip/>
					<el-table-column label="流地址" width="400" align="center" show-overflow-tooltip >
						<template slot-scope="scope">
						<div slot="reference" class="name-wrapper">
							
							<el-tag size="medium" v-if="scope.row.type == 'default'">
								<i class="cpoy-btn el-icon-document-copy"  title="点击拷贝" v-clipboard="scope.row.url" @success="$message({type:'success', message:'成功拷贝到粘贴板'})"></i> 
								{{scope.row.url}}
							</el-tag>
							<el-tag size="medium" v-if="scope.row.type != 'default'">
								<i class="cpoy-btn el-icon-document-copy"  title="点击拷贝" v-clipboard="scope.row.src_url" @success="$message({type:'success', message:'成功拷贝到粘贴板'})"></i> 
								{{scope.row.src_url}}
							</el-tag>
						</div>
						</template>
					</el-table-column>
					<el-table-column prop="gbId" label="国标编码" width="180" align="center" show-overflow-tooltip/>
					<el-table-column label="转HLS" width="120" align="center">
						<template slot-scope="scope">
						<div slot="reference" class="name-wrapper">
							<el-tag size="medium" v-if="scope.row.enable_hls">已启用</el-tag>
							<el-tag size="medium" type="info" v-if="!scope.row.enable_hls">未启用</el-tag>
						</div>
						</template>
					</el-table-column>
					<el-table-column label="MP4录制" width="120" align="center">
						<template slot-scope="scope">
						<div slot="reference" class="name-wrapper">
							<el-tag size="medium" v-if="scope.row.enable_mp4">已启用</el-tag>
							<el-tag size="medium" type="info" v-if="!scope.row.enable_mp4">未启用</el-tag>
						</div>
						</template>
					</el-table-column>
					<el-table-column label="启用" width="120" align="center">
						<template slot-scope="scope">
						<div slot="reference" class="name-wrapper">
							<el-tag size="medium" v-if="scope.row.enable">已启用</el-tag>
							<el-tag size="medium" type="info" v-if="!scope.row.enable">未启用</el-tag>
						</div>
						</template>
					</el-table-column>

					<el-table-column label="操作" width="360" align="center" fixed="right">
						<template slot-scope="scope">
							<el-button-group>
								<el-button size="mini" icon="el-icon-video-play" v-if="scope.row.enable" @click="play(scope.row)">播放</el-button>
								<el-button size="mini" icon="el-icon-close" type="success" v-if="scope.row.enable" @click="stop(scope.row)">停用</el-button>
								<el-button size="mini" icon="el-icon-check" type="primary" v-if="!scope.row.enable" @click="start(scope.row)">启用</el-button>
								<el-button size="mini" icon="el-icon-delete" type="danger"  @click="deleteStreamProxy(scope.row)">删除</el-button>
								<!-- <el-button size="mini" icon="el-icon-position" type="primary"  >加入国标</el-button> -->
							</el-button-group>
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
			</el-main>
		</el-container>
	</div>
</template>

<script>
	import streamProxyEdit from './dialog/StreamProxyEdit.vue'
	import devicePlayer from './dialog/devicePlayer.vue'
	import uiHeader from './UiHeader.vue'
	export default {
		name: 'streamProxyList',
		components: {
			devicePlayer,
			streamProxyEdit,
			uiHeader
		},
		data() {
			return {
				streamProxyList: [], 
				currentPusher: {}, //当前操作设备对象
				updateLooper: 0, //数据刷新轮训标志
				currentDeviceChannelsLenth:0,
				winHeight: window.innerHeight - 200,
				currentPage:1,
				count:15,
				total:0,
				getListLoading: false
			};
		},
		computed: {
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
				this.getStreamProxyList();
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
				this.getListLoading = true;
				this.$axios.get(`/api/proxy/list`,{
					params: {
						page: that.currentPage,
						count: that.count
					}
				} )
				.then(function (res) {
					console.log(res);
					console.log(res.data.list);
					that.total = res.data.total;
					that.streamProxyList = res.data.list;
					that.getListLoading = false;
				})
				.catch(function (error) {
					console.log(error);
					that.getListLoading = false;
				});
			},
			addStreamProxy: function(){
				this.$refs.streamProxyEdit.openDialog(null, this.initData)
			},
			saveStreamProxy: function(){
			},
			play: function(row){
				let that = this;
				this.getListLoading = true;
				this.$axios.get(`/api/media/getStreamInfoByAppAndStream`,{
					params: {
						app: row.app,
						stream: row.stream
					}
				})
				.then(function (res) {
					that.getListLoading = false;
					that.$refs.devicePlayer.openDialog("streamPlay", null, null, {
                        streamInfo: res.data,
                        hasAudio: true
                    });
				})
				.catch(function (error) {
					console.log(error);
					that.getListLoading = false;
				});
				
			},
			deleteStreamProxy: function(row){
				console.log(1111)
				let that = this;
				this.getListLoading = true;
				this.$axios.get(`/api/proxy/del`,{
					params: {
						app: row.app,
						stream: row.stream
					}
				})
				.then(function (res) {
					that.getListLoading = false;
					that.initData()
				})
				.catch(function (error) {
					console.log(error);
					that.getListLoading = false;
				});
			},
			start: function(row){
				let that = this;
				this.getListLoading = true;
				this.$axios.get(`/api/proxy/start`,{
					params: {
						app: row.app,
						stream: row.stream
					}
				})
				.then(function (res) {
					that.getListLoading = false;
					that.initData()
				})
				.catch(function (error) {
					console.log(error);
					that.getListLoading = false;
				});
			},
			stop: function(row){
				let that = this;
				this.getListLoading = true;
				this.$axios.get(`/api/proxy/stop`,{
					params: {
						app: row.app,
						stream: row.stream
					}
				})
				.then(function (res) {
					that.getListLoading = false;
					that.initData()
				})
				.catch(function (error) {
					console.log(error);
					that.getListLoading = false;
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
	.cpoy-btn {
		cursor: pointer;
		margin-right: 10px;
	}
</style>
