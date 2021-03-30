<template>
	<div id="pushVideoList">
		<el-container>
			<el-header>
				<uiHeader></uiHeader>
			</el-header>
			<el-main>
				<div style="background-color: #FFFFFF; margin-bottom: 1rem; position: relative; padding: 0.5rem; text-align: left;">
					<span style="font-size: 1rem; font-weight: bold;">推流列表</span>
				</div>
				<div style="background-color: #FFFFFF; margin-bottom: 1rem; position: relative; padding: 0.5rem; text-align: left;font-size: 14px;">
					<el-button icon="el-icon-plus" size="mini" style="margin-right: 1rem;" type="primary" @click="addStreamProxy">添加代理</el-button>
				</div>
				<devicePlayer ref="devicePlayer"></devicePlayer>
				<el-table :data="pushList" border style="width: 100%" :height="winHeight">
					<el-table-column prop="app" label="APP" width="180" align="center">
					</el-table-column>
					<el-table-column prop="stream" label="流ID" width="240" align="center">
					</el-table-column>
					<el-table-column prop="totalReaderCount" label="在线人数" width="240" align="center">
					</el-table-column>
					<el-table-column prop="createStamp" label="开始时间" align="center">
					</el-table-column>
					

					<el-table-column label="操作" width="360" align="center" fixed="right">
						<template slot-scope="scope">
							<el-button-group>
								<el-button size="mini" icon="el-icon-video-play" @click="playPuhsh(scope.row)">播放</el-button>
								<el-button size="mini" icon="el-icon-switch-button" type="danger" v-if="!!scope.row.streamId" @click="stopPuhsh(scope.row)">停止</el-button>
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
		name: 'pushVideoList',
		components: {
			devicePlayer,
			streamProxyEdit,
			uiHeader
		},
		data() {
			return {
				pushList: [], //设备列表
				currentPusher: {}, //当前操作设备对象
				updateLooper: 0, //数据刷新轮训标志
				currentDeviceChannelsLenth:0,
				winHeight: window.innerHeight - 200,
				currentPage:1,
				count:15,
				total:0,
				getDeviceListLoading: false
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
				this.getPushList();
			},
			currentChange: function(val){
				this.currentPage = val;
				this.getPushList();
			},
			handleSizeChange: function(val){
				this.count = val;
				this.getPushList();
			},
			getPushList: function() {
				let that = this;
				this.getDeviceListLoading = true;
				this.$axios.get(`/api/media/list`,{
					params: {
						page: that.currentPage,
						count: that.count
					}
				} )
				.then(function (res) {
					console.log(res);
					console.log(res.data.list);
					that.total = res.data.total;
					that.pushList = res.data.list;
					that.getDeviceListLoading = false;
				})
				.catch(function (error) {
					console.log(error);
					that.getDeviceListLoading = false;
				});
			},
			addStreamProxy: function(){
				console.log(2222)
				this.$refs.streamProxyEdit.openDialog(null, this.initData)
			},
			saveStreamProxy: function(){
			},
			playPuhsh: function(row){
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
			stopPuhsh: function(row){
				console.log(row)
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
