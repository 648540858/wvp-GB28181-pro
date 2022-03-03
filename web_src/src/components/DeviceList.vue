<template>
	<div id="app">
		<el-container>
			<el-header>
				<uiHeader></uiHeader>
			</el-header>
			<el-main>
				<div style="background-color: #FFFFFF; margin-bottom: 1rem; position: relative; padding: 0.5rem; text-align: left;">
					<span style="font-size: 1rem; font-weight: bold;">设备列表</span>
					<div style="position: absolute; right: 1rem; top: 0.3rem;">
						<el-button icon="el-icon-refresh-right" circle size="mini" :loading="getDeviceListLoading" @click="getDeviceList()"></el-button>
					</div>
				</div>
				<!-- <devicePlayer ref="devicePlayer"></devicePlayer> -->
				<!--设备列表-->
				<el-table :data="deviceList" border style="width: 100%;font-size: 12px;" :height="winHeight">
					<el-table-column prop="name" label="名称"  align="center">
					</el-table-column>
					<el-table-column prop="deviceId" label="设备编号" width="180" align="center">
					</el-table-column>
          <el-table-column label="地址" width="180" align="center">
            <template slot-scope="scope">
              <div slot="reference" class="name-wrapper">
                <el-tag size="medium">{{ scope.row.hostAddress }}</el-tag>
              </div>
            </template>
          </el-table-column>
					<el-table-column prop="manufacturer" label="厂家" align="center">
					</el-table-column>
					<el-table-column label="流传输模式" align="center" width="120">
            <template slot-scope="scope">
              <el-select size="mini" @change="transportChange(scope.row)" v-model="scope.row.streamMode" placeholder="请选择">
                <el-option key="UDP" label="UDP" value="UDP"></el-option>
                <el-option key="TCP-ACTIVE" label="TCP主动模式" :disabled="true" value="TCP-ACTIVE"></el-option>
                <el-option key="TCP-PASSIVE" label="TCP被动模式"  value="TCP-PASSIVE"></el-option>
              </el-select>
            </template>
					</el-table-column>
					<el-table-column prop="channelCount" label="通道数" align="center">
					</el-table-column>
					<el-table-column label="状态" width="120" align="center">
						<template slot-scope="scope">
							<div slot="reference" class="name-wrapper">
								<el-tag size="medium" v-if="scope.row.online == 1">在线</el-tag>
								<el-tag size="medium" type="info" v-if="scope.row.online == 0">离线</el-tag>
							</div>
						</template>
					</el-table-column>
          <el-table-column prop="keepaliveTime" label="最近心跳" align="center" width="140">
          </el-table-column>
          <el-table-column prop="registerTime" label="最近注册" align="center" width="140">
          </el-table-column>
          <el-table-column prop="updateTime" label="更新时间" align="center" width="140">
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" align="center" width="140">
          </el-table-column>

					<el-table-column label="操作" width="450" align="center" fixed="right">
						<template slot-scope="scope">
							<el-button size="mini" :ref="scope.row.deviceId + 'refbtn' "  v-if="scope.row.online!=0" icon="el-icon-refresh"  @click="refDevice(scope.row)">刷新</el-button>
							<el-button-group>
                <el-button size="mini" icon="el-icon-video-camera-solid" v-bind:disabled="scope.row.online==0"  type="primary" @click="showChannelList(scope.row)">通道</el-button>
                <el-button size="mini" icon="el-icon-location" v-bind:disabled="scope.row.online==0"  type="primary" @click="showDevicePosition(scope.row)">定位</el-button>
                <el-button size="mini" icon="el-icon-edit" type="primary" @click="edit(scope.row)">编辑</el-button>
                <el-button size="mini" icon="el-icon-delete" type="danger" @click="deleteDevice(scope.row)">删除</el-button>
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
        <deviceEdit ref="deviceEdit" ></deviceEdit>
			</el-main>
		</el-container>
	</div>
</template>

<script>
	import uiHeader from './UiHeader.vue'
	import deviceEdit from './dialog/deviceEdit.vue'
	export default {
		name: 'app',
		components: {
			uiHeader,
      deviceEdit
		},
		data() {
			return {
				deviceList: [], //设备列表
				currentDevice: {}, //当前操作设备对象

				videoComponentList: [],
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
			getcurrentDeviceChannels: function() {
				let data = this.currentDevice['channelMap'];
				let channels = null;
				if (data) {
					channels = Object.keys(data).map(key => {
						return data[key];
					});
					this.currentDeviceChannelsLenth = channels.length;
				}

				console.log("数据：" + JSON.stringify(channels));
				return channels;
			}
		},
		mounted() {
			this.initData();
			this.updateLooper = setInterval(this.initData, 10000);
		},
		destroyed() {
			this.$destroy('videojs');
			clearTimeout(this.updateLooper);
		},
		methods: {
			initData: function() {
				this.getDeviceList();
			},
			currentChange: function(val){
				this.currentPage = val;
				this.getDeviceList();
			},
			handleSizeChange: function(val){
				this.count = val;
				this.getDeviceList();
			},
			getDeviceList: function() {
				let that = this;
				this.getDeviceListLoading = true;
				this.$axios({
					method: 'get',
					url:`/api/device/query/devices`,
					params: {
						page: that.currentPage,
						count: that.count
					}
				}).then(function (res) {
					console.log(res);
					console.log(res.data.list);
					that.total = res.data.total;
					that.deviceList = res.data.list;
					that.getDeviceListLoading = false;
				}).catch(function (error) {
					console.log(error);
					that.getDeviceListLoading = false;
				});

			},
      deleteDevice: function(row) {
        let msg = "确定删除此设备？"
        if (row.online !== 0) {
          msg = "在线设备删除后仍可通过注册再次上线。<br/>如需彻底删除请先将设备离线。<br/><strong>确定删除此设备？</strong>"
        }
        this.$confirm(msg, '提示', {
          dangerouslyUseHTMLString : true,
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          center: true,
          type: 'warning'
        }).then(() => {
          this.$axios({
            method: 'delete',
            url:`/api/device/query/devices/${row.deviceId}/delete`
          }).then((res)=>{
            this.getDeviceList();
          }).catch((error) =>{
            console.log(error);
          });
        }).catch(() => {

        });


			},
			showChannelList: function(row) {
				console.log(JSON.stringify(row))
				this.$router.push(`/channelList/${row.deviceId}/0/15/1`);
			},
			showDevicePosition: function(row) {
				console.log(JSON.stringify(row))
				this.$router.push(`/devicePosition/${row.deviceId}/0/15/1`);
			},

			//gb28181平台对接
			//刷新设备信息
			refDevice: function(itemData) {
				console.log("刷新对应设备:" + itemData.deviceId);
				var that = this;
				that.$refs[itemData.deviceId + 'refbtn' ].loading = true;
				this.$axios({
					method: 'post',
					url: '/api/device/query/devices/' + itemData.deviceId + '/sync'
				}).then(function(res) {
					console.log("刷新设备结果："+JSON.stringify(res));
					if (res.data.code !==0) {
						that.$message({
							showClose: true,
							message: res.data.msg,
							type: 'error'
						});
					}else{
						that.$message({
							showClose: true,
							message: res.data.msg,
							type: 'success'
						});
					}
					that.initData()
					that.$refs[itemData.deviceId + 'refbtn' ].loading = false;
				}).catch(function(e) {
					console.error(e)
          that.$message({
            showClose: true,
            message: e,
            type: 'error'
          });
					that.$refs[itemData.deviceId + 'refbtn' ].loading = false;
				});
			},
			//通知设备上传媒体流
			sendDevicePush: function(itemData) {
				// let deviceId = this.currentDevice.deviceId;
				// let channelId = itemData.channelId;
				// console.log("通知设备推流1：" + deviceId + " : " + channelId);
				// let that = this;
				// this.$axios({
				// 	method: 'get',
				// 	url: '/api/play/' + deviceId + '/' + channelId
				// }).then(function(res) {
				// 	let ssrc = res.data.ssrc;
				// 	that.$refs.devicePlayer.play(ssrc,deviceId,channelId);
				// }).catch(function(e) {
				// });
			},
      transportChange: function (row) {
        console.log(row);
        console.log(`修改传输方式为 ${row.streamMode}：${row.deviceId} `);
        let that = this;
        this.$axios({
          method: 'post',
          url: '/api/device/query/transport/' + row.deviceId + '/' + row.streamMode
        }).then(function(res) {

        }).catch(function(e) {
        });
      },
      edit: function (row) {
        console.log(row);
        this.$refs.deviceEdit.openDialog(row, ()=>{
          this.$refs.deviceEdit.close();
          this.$message({
            showClose: true,
            message: "设备修改成功，通道字符集将在下次更新生效",
            type: "success",
          });
          setTimeout(this.getDeviceList, 200)

        })
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
