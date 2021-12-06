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
				<devicePlayer ref="devicePlayer"></devicePlayer>
				<addStreamTOGB ref="addStreamTOGB"></addStreamTOGB>
				<el-table :data="pushList" border style="width: 100%" :height="winHeight">
					<el-table-column prop="app" label="APP" width="180" align="center">
					</el-table-column>
					<el-table-column prop="stream" label="流ID" width="240" align="center">
					</el-table-column>
					<el-table-column prop="gbId" label="国标编码" width="150" align="center">
					</el-table-column>
					<el-table-column prop="mediaServerId" label="流媒体" width="150" align="center">
					</el-table-column>
					<el-table-column label="开始时间" align="center" >
						<template slot-scope="scope">
							<el-button-group>
								{{dateFormat(parseInt(scope.row.createStamp))}}
							</el-button-group>
							</template>
					</el-table-column>
					<el-table-column label="正在推流" align="center" >
						<template slot-scope="scope">
							{{(scope.row.status == false && scope.row.gbId == null) || scope.row.status ?'是':'否'}}
						</template>
					</el-table-column>

					<el-table-column label="操作" width="360" align="center" fixed="right">
						<template slot-scope="scope">
							<el-button-group>
								<el-button size="mini" icon="el-icon-video-play" v-if="scope.row.status" @click="playPuhsh(scope.row)">播放</el-button>
								<el-button size="mini" icon="el-icon-switch-button" type="danger" @click="stopPuhsh(scope.row)">移除</el-button>
								<el-button size="mini" icon="el-icon-position" type="primary" v-if="!!!scope.row.gbId" @click="addToGB(scope.row)">加入国标</el-button>
								<el-button size="mini" icon="el-icon-position" type="primary" v-if="!!scope.row.gbId" @click="removeFromGB(scope.row)">移出国标</el-button>
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
	import addStreamTOGB from './dialog/addStreamTOGB.vue'
	import uiHeader from './UiHeader.vue'
	export default {
		name: 'pushVideoList',
		components: {
			devicePlayer,
			addStreamTOGB,
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
			this.updateLooper = setInterval(this.initData, 2000);
		},
		destroyed() {
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
				this.$axios({
					method: 'get',
					url:`/api/push/list`,
					params: {
						page: that.currentPage,
						count: that.count
					}
				}).then(function (res) {
					console.log(res);
					console.log(res.data.list);
					that.total = res.data.total;
					that.pushList = res.data.list;
					that.getDeviceListLoading = false;
				}).catch(function (error) {
					console.log(error);
					that.getDeviceListLoading = false;
				});
			},

			playPuhsh: function(row){
				let that = this;
				this.getListLoading = true;
				this.$axios({
					method: 'get',
					url: '/api/media/stream_info_by_app_and_stream',
					params: {
						app: row.app,
						stream: row.stream,
            mediaServerId: row.mediaServerId
					}
				}).then(function (res) {
					that.getListLoading = false;
					that.$refs.devicePlayer.openDialog("streamPlay", null, null, {
                        streamInfo: res.data.data,
                        hasAudio: true
                    });
				}).catch(function (error) {
					console.log(error);
					that.getListLoading = false;
				});
			},
			stopPuhsh: function(row){
        var that = this;
        that.$axios({
          method:"post",
          url:"/api/push/stop",
          params: {
            app: row.app,
            streamId: row.stream
          }
        }).then((res)=>{
          if (res.data == "success") {
            that.initData()
          }
        }).catch(function (error) {
          console.log(error);
        });
			},
			addToGB: function(row){
				this.$refs.addStreamTOGB.openDialog({app: row.app, stream: row.stream, mediaServerId: row.mediaServerId}, this.initData);
			},
			removeFromGB: function(row){
				var that = this;
				that.$axios({
            method:"delete",
            url:"/api/push/remove_form_gb",
            data:row
        }).then((res)=>{
            if (res.data == "success") {
							that.initData()
						}
        }).catch(function (error) {
            console.log(error);
        });
			},
			dateFormat: function(/** timestamp=0 **/) {
				var ts = arguments[0] || 0;
				var t,y,m,d,h,i,s;
				t = ts ? new Date(ts*1000) : new Date();
				y = t.getFullYear();
				m = t.getMonth()+1;
				d = t.getDate();
				h = t.getHours();
				i = t.getMinutes();
				s = t.getSeconds();
				// 可根据需要在这里定义时间格式
				return y+'-'+(m<10?'0'+m:m)+'-'+(d<10?'0'+d:d)+' '+(h<10?'0'+h:h)+':'+(i<10?'0'+i:i)+':'+(s<10?'0'+s:s);
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
