<template>
<div id="devicePlayer" v-loading="isLoging">

    <el-dialog title="视频播放" top="0" :close-on-click-modal="false" :visible.sync="showVideoDialog" @close="close()">
        <!-- <LivePlayer v-if="showVideoDialog" ref="videoPlayer" :videoUrl="videoUrl" :error="videoError" :message="videoError" :hasaudio="hasaudio" fluent autoplay live></LivePlayer> -->
        <player ref="videoPlayer" :visible.sync="showVideoDialog" :videoUrl="videoUrl" :error="videoError" :message="videoError" :height="false" :hasAudio="hasAudio" fluent autoplay live ></player>
        <div id="shared" style="text-align: right; margin-top: 1rem;">
            <el-tabs v-model="tabActiveName" @tab-click="tabHandleClick">
                <el-tab-pane label="实时视频" name="media">
                    <div style="margin-bottom: 0.5rem;">
                        <!--		<el-button type="primary" size="small" @click="playRecord(true, '')">播放</el-button>-->
                        <!--		 <el-button type="primary" size="small" @click="startRecord()">录制</el-button>-->
                        <!--		 <el-button type="primary" size="small" @click="stopRecord()">停止录制</el-button>-->
                    </div>
                    <div style="display: flex; margin-bottom: 0.5rem; height: 2.5rem;">
                        <span style="width: 5rem; line-height: 2.5rem; text-align: right;">播放地址：</span>
                        <el-input v-model="getPlayerShared.sharedUrl" :disabled="true" >
                          <template slot="append">
                            <i class="cpoy-btn el-icon-document-copy"  title="点击拷贝" v-clipboard="getPlayerShared.sharedUrl" @success="$message({type:'success', message:'成功拷贝到粘贴板'})"></i>
                          </template>
                        </el-input>
                    </div>
                    <div style="display: flex; margin-bottom: 0.5rem; height: 2.5rem;">
                        <span style="width: 5rem; line-height: 2.5rem; text-align: right;">iframe：</span>
                        <el-input v-model="getPlayerShared.sharedIframe" :disabled="true" >
                          <template slot="append">
                            <i class="cpoy-btn el-icon-document-copy"  title="点击拷贝" v-clipboard="getPlayerShared.sharedIframe" @success="$message({type:'success', message:'成功拷贝到粘贴板'})"></i>
                          </template>
                        </el-input>
                    </div>
                    <div style="display: flex; margin-bottom: 0.5rem; height: 2.5rem;">
                        <span style="width: 5rem; line-height: 2.5rem; text-align: right;">资源地址：</span>
                        <el-input v-model="getPlayerShared.sharedRtmp" :disabled="true" >
                          <template slot="append">
                            <i class="cpoy-btn el-icon-document-copy"  title="点击拷贝" v-clipboard="getPlayerShared.sharedRtmp" @success="$message({type:'success', message:'成功拷贝到粘贴板'})"></i>
                          </template>
                        </el-input>
                    </div>
                </el-tab-pane>
                <!--{"code":0,"data":{"paths":["22-29-30.mp4"],"rootPath":"/home/kkkkk/Documents/ZLMediaKit/release/linux/Debug/www/record/hls/kkkkk/2020-05-11/"}}-->
                <el-tab-pane label="录像查询" name="record" v-if="showRrecord">
                    <div style="width: 100%;">
                      <div style="width: 100%; text-align: left">
                        <span>录像控制</span>
                        <el-button-group style="margin-left: 1rem;">
                          <el-button size="mini" class="iconfont icon-zanting" title="开始" @click="gbPause()"></el-button>
                          <el-button size="mini" class="iconfont icon-kaishi" title="暂停" @click="gbPlay()"></el-button>
                          <el-dropdown size="mini" title="播放倍速" style="margin-left: 1px;" @command="gbScale">
                            <el-button size="mini">
                              倍速 <i class="el-icon-arrow-down el-icon--right"></i>
                            </el-button>
                            <el-dropdown-menu  slot="dropdown">
                              <el-dropdown-item command="0.25">0.25倍速</el-dropdown-item>
                              <el-dropdown-item command="0.5">0.5倍速</el-dropdown-item>
                              <el-dropdown-item command="1.0">1倍速</el-dropdown-item>
                              <el-dropdown-item command="2.0">2倍速</el-dropdown-item>
                              <el-dropdown-item command="4.0">4倍速</el-dropdown-item>
                            </el-dropdown-menu>
                          </el-dropdown>
                        </el-button-group>
                        <el-date-picker style="float: right;" size="mini" v-model="videoHistory.date" type="date" value-format="yyyy-MM-dd" placeholder="日期" @change="queryRecords()"></el-date-picker>
                      </div>
                      <div style="width: 100%; text-align: left">
                        <span class="demonstration" style="padding: 12px 36px 12px 0;float: left;">{{showTimeText}}</span>
                        <el-slider style="width: 80%; float:left;" v-model="sliderTime" @change="gbSeek" :show-tooltip="false"></el-slider>
                      </div>
                    </div>


                    <el-table :data="videoHistory.searchHistoryResult" height="150" v-loading="recordsLoading">
                        <el-table-column label="名称" prop="name"></el-table-column>
                        <el-table-column label="文件" prop="filePath"></el-table-column>
                        <el-table-column label="开始时间" prop="startTime" :formatter="timeFormatter"></el-table-column>
                        <el-table-column label="结束时间" prop="endTime" :formatter="timeFormatter"></el-table-column>

                        <el-table-column label="操作">
                            <template slot-scope="scope">
                                <el-button-group>
                                    <el-button icon="el-icon-video-play" size="mini" @click="playRecord(scope.row)">播放</el-button>
                                    <el-button icon="el-icon-download" size="mini" @click="downloadRecord(scope.row)">下载</el-button>
                                </el-button-group>
                            </template>
                        </el-table-column>
                    </el-table>
                </el-tab-pane>
                <!--遥控界面-->
                <el-tab-pane label="云台控制" name="control" v-if="showPtz">
                    <div style="display: flex; justify-content: left;">
                        <div class="control-wrapper">
                            <div class="control-btn control-top" @mousedown="ptzCamera('up')" @mouseup="ptzCamera('stop')">
                                <i class="el-icon-caret-top"></i>
                                <div class="control-inner-btn control-inner"></div>
                            </div>
                            <div class="control-btn control-left" @mousedown="ptzCamera('left')" @mouseup="ptzCamera('stop')">
                                <i class="el-icon-caret-left"></i>
                                <div class="control-inner-btn control-inner"></div>
                            </div>
                            <div class="control-btn control-bottom" @mousedown="ptzCamera('down')" @mouseup="ptzCamera('stop')">
                                <i class="el-icon-caret-bottom"></i>
                                <div class="control-inner-btn control-inner"></div>
                            </div>
                            <div class="control-btn control-right" @mousedown="ptzCamera('right')" @mouseup="ptzCamera('stop')">
                                <i class="el-icon-caret-right"></i>
                                <div class="control-inner-btn control-inner"></div>
                            </div>
                            <div class="control-round">
                                <div class="control-round-inner"><i class="fa fa-pause-circle"></i></div>
                            </div>
                            <div style="position: absolute; left: 7.25rem; top: 1.25rem" @mousedown="ptzCamera('zoomin')" @mouseup="ptzCamera('stop')"><i class="el-icon-zoom-in control-zoom-btn" style="font-size: 1.875rem;"></i></div>
                            <div style="position: absolute; left: 7.25rem; top: 3.25rem; font-size: 1.875rem;" @mousedown="ptzCamera('zoomout')" @mouseup="ptzCamera('stop')"><i class="el-icon-zoom-out control-zoom-btn"></i></div>
                             <div class="contro-speed" style="position: absolute; left: 4px; top: 7rem; width: 9rem;">
                                 <el-slider v-model="controSpeed" :max="255"></el-slider>
                             </div>
                        </div>

                        <div class="control-panel">
                            <el-button-group>
                                <el-tag style="position :absolute; left: 0rem; top: 0rem; width: 5rem; text-align: center" size="medium" type="info">预置位编号</el-tag>
                                <el-input-number style="position: absolute; left: 5rem; top: 0rem; width: 6rem" size="mini" v-model="presetPos" controls-position="right" :precision="0" :step="1" :min="1" :max="255"></el-input-number>
                                <el-button style="position: absolute; left: 11rem; top: 0rem; width: 5rem" size="mini" icon="el-icon-add-location" @click="presetPosition(129, presetPos)">设置</el-button>
                                <el-button style="position: absolute; left: 27rem; top: 0rem; width: 5rem" size="mini" type="primary" icon="el-icon-place" @click="presetPosition(130, presetPos)">调用</el-button>
                                <el-button style="position: absolute; left: 16rem; top: 0rem; width: 5rem" size="mini" icon="el-icon-delete-location" @click="presetPosition(131, presetPos)">删除</el-button>
                                <el-tag style="position :absolute; left: 0rem; top: 2.5rem; width: 5rem; text-align: center" size="medium" type="info">巡航速度</el-tag>
                                <el-input-number style="position: absolute; left: 5rem; top: 2.5rem; width: 6rem" size="mini" v-model="cruisingSpeed" controls-position="right" :precision="0" :min="1" :max="4095"></el-input-number>
                                <el-button style="position: absolute; left: 11rem; top: 2.5rem; width: 5rem" size="mini" icon="el-icon-loading" @click="setSpeedOrTime(134, cruisingGroup, cruisingSpeed)">设置</el-button>
                                <el-tag style="position :absolute; left: 16rem; top: 2.5rem; width: 5rem; text-align: center" size="medium" type="info">停留时间</el-tag>
                                <el-input-number style="position: absolute; left: 21rem; top: 2.5rem; width: 6rem" size="mini" v-model="cruisingTime" controls-position="right" :precision="0" :min="1" :max="4095"></el-input-number>
                                <el-button style="position: absolute; left: 27rem; top: 2.5rem; width: 5rem" size="mini" icon="el-icon-timer" @click="setSpeedOrTime(135, cruisingGroup, cruisingTime)">设置</el-button>
                                <el-tag style="position :absolute; left: 0rem; top: 4.5rem; width: 5rem; text-align: center" size="medium" type="info">巡航组编号</el-tag>
                                <el-input-number style="position: absolute; left: 5rem; top: 4.5rem; width: 6rem" size="mini" v-model="cruisingGroup" controls-position="right" :precision="0" :min="0" :max="255"></el-input-number>
                                <el-button style="position: absolute; left: 11rem; top: 4.5rem; width: 5rem" size="mini" icon="el-icon-add-location" @click="setCommand(132, cruisingGroup, presetPos)">添加点</el-button>
                                <el-button style="position: absolute; left: 16rem; top: 4.5rem; width: 5rem" size="mini" icon="el-icon-delete-location" @click="setCommand(133, cruisingGroup, presetPos)">删除点</el-button>
                                <el-button style="position: absolute; left: 21rem; top: 4.5rem; width: 5rem" size="mini" icon="el-icon-delete" @click="setCommand(133, cruisingGroup, 0)">删除组</el-button>
                                <el-button style="position: absolute; left: 27rem; top: 5rem; width: 5rem" size="mini" type="primary" icon="el-icon-video-camera-solid" @click="setCommand(136, cruisingGroup, 0)">巡航</el-button>
                                <el-tag style="position :absolute; left: 0rem; top: 7rem; width: 5rem; text-align: center" size="medium" type="info">扫描速度</el-tag>
                                <el-input-number style="position: absolute; left: 5rem; top: 7rem; width: 6rem" size="mini" v-model="scanSpeed" controls-position="right" :precision="0" :min="1" :max="4095"></el-input-number>
                                <el-button style="position: absolute; left: 11rem; top: 7rem; width: 5rem" size="mini" icon="el-icon-loading" @click="setSpeedOrTime(138, scanGroup, scanSpeed)">设置</el-button>
                                <el-tag style="position :absolute; left: 0rem; top: 9rem; width: 5rem; text-align: center" size="medium" type="info">扫描组编号</el-tag>
                                <el-input-number style="position: absolute; left: 5rem; top: 9rem; width: 6rem" size="mini" v-model="scanGroup" controls-position="right" :precision="0" :step="1" :min="0" :max="255"></el-input-number>
                                <el-button style="position: absolute; left: 11rem; top: 9rem; width: 5rem" size="mini" icon="el-icon-d-arrow-left" @click="setCommand(137, scanGroup, 1)">左边界</el-button>
                                <el-button style="position: absolute; left: 16rem; top: 9rem; width: 5rem" size="mini" icon="el-icon-d-arrow-right" @click="setCommand(137, scanGroup, 2)">右边界</el-button>
                                <el-button style="position: absolute; left: 27rem; top: 7rem; width: 5rem" size="mini" type="primary" icon="el-icon-video-camera-solid" @click="setCommand(137, scanGroup, 0)">扫描</el-button>
                                <el-button style="position: absolute; left: 27rem; top: 9rem; width: 5rem" size="mini" type="danger" icon="el-icon-switch-button" @click="ptzCamera('stop')">停止</el-button>
                            </el-button-group>
                        </div>
                    </div>
                </el-tab-pane>
                <el-tab-pane label="编码信息" name="codec" v-loading="tracksLoading">
                    <p>
                        无法播放或者没有声音?&nbsp&nbsp&nbsp试一试&nbsp
                        <el-button size="mini" type="primary" v-if="!coverPlaying" @click="coverPlay">转码播放</el-button>
                        <el-button size="mini" type="danger" v-if="coverPlaying" @click="convertStopClick">停止转码</el-button>
                    </p>
                    <div class="trank" >
                      <p v-if="tracksNotLoaded" style="text-align: center;padding-top: 3rem;">暂无数据</p>
                        <div v-for="(item, index) in tracks" style="width: 50%; float: left" loading>
                            <span >流 {{index}}</span>
                            <div class="trankInfo" v-if="item.codec_type == 0">
                                <p>格式: {{item.codec_id_name}}</p>
                                <p>类型: 视频</p>
                                <p>分辨率: {{item.width}} x {{item.height}}</p>
                                <p>帧率: {{item.fps}}</p>
                            </div>
                            <div class="trankInfo" v-if="item.codec_type == 1">
                                <p>格式: {{item.codec_id_name}}</p>
                                <p>类型: 音频</p>
                                <p>采样位数: {{item.sample_bit}}</p>
                                <p>采样率: {{item.sample_rate}}</p>
                            </div>
                        </div>

                    </div>

                </el-tab-pane>
            </el-tabs>
        </div>
    </el-dialog>
</div>
</template>

<script>
// import player from '../dialog/rtcPlayer.vue'
// import LivePlayer from '@liveqing/liveplayer'
// import player from '../dialog/easyPlayer.vue'
import player from '../dialog/jessibuca.vue'
export default {
    name: 'devicePlayer',
    props: {},
    components: {
        player,
    },
    computed: {
        getPlayerShared: function () {

            return {
                sharedUrl: window.location.origin + '/#/play/wasm/' + encodeURIComponent(this.videoUrl),
                sharedIframe: '<iframe src="' + window.location.origin + '/#/play/wasm/' + encodeURIComponent(this.videoUrl) + '"></iframe>',
                sharedRtmp: this.videoUrl
            };
        }
    },
    created() {},
    data() {
        return {
            video: 'http://lndxyj.iqilu.com/public/upload/2019/10/14/8c001ea0c09cdc59a57829dabc8010fa.mp4',
            videoUrl: '',
            videoHistory: {
                date: '',
                searchHistoryResult: [] //媒体流历史记录搜索结果
            },
            showVideoDialog: false,
            streamId: '',
            app : '',
            mediaServerId : '',
            convertKey: '',
            deviceId: '',
            channelId: '',
            tabActiveName: 'media',
            hasAudio: false,
            loadingRecords: false,
            recordsLoading: false,
            isLoging: false,
            controSpeed: 30,
            timeVal: 0,
            timeMin: 0,
            timeMax: 1440,
            presetPos: 1,
            cruisingSpeed: 100,
            cruisingTime: 5,
            cruisingGroup: 0,
            scanSpeed: 100,
            scanGroup: 0,
            tracks: [],
            coverPlaying:false,
            tracksLoading: false,
            recordPlay: "",
            showPtz: true,
            showRrecord: true,
            tracksNotLoaded: false,
            sliderTime: 0,
            seekTime: 0,
            recordStartTime: 0,
            showTimeText: "00:00:00",
        };
    },
    methods: {
        tabHandleClick: function(tab, event) {
            console.log(tab)
            var that = this;
            that.tracks = [];
            that.tracksLoading = true;
            that.tracksNotLoaded = false;
            if (tab.name == "codec") {
                this.$axios({
                    method: 'get',
                    url: '/zlm/' +this.mediaServerId+ '/index/api/getMediaInfo?vhost=__defaultVhost__&schema=rtmp&app='+ this.app +'&stream='+ this.streamId
                }).then(function (res) {
                    that.tracksLoading = false;
                    if (res.data.code == 0 && res.data.online) {
                        that.tracks = res.data.tracks;
                    }else{
                        that.tracksNotLoaded = true;
                        that.$message({
                            showClose: true,
                            message: '获取编码信息失败,',
                            type: 'warning'
                        });
                    }
                }).catch(function (e) {});
            }
        },
        openDialog: function (tab, deviceId, channelId, param) {
            this.tabActiveName = tab;
            this.channelId = channelId;
            this.deviceId = deviceId;
            this.streamId = "";
            this.mediaServerId = "";
            this.app = "";
            this.videoUrl = ""
            if (!!this.$refs.videoPlayer) {
                this.$refs.videoPlayer.pause();
            }
            switch (tab) {
                case "media":
                    this.play(param.streamInfo, param.hasAudio)
                    break;
                case "record":
                    this.showVideoDialog = true;
                    this.videoHistory.date = param.date;
                    this.queryRecords()
                    break;
                case "streamPlay":
                    this.tabActiveName = "media";
                    this.showRrecord = false;
                    this.showPtz = false;
                    this.play(param.streamInfo, param.hasAudio)
                    break;
                case "control":
                    break;
            }
        },
        timeAxisSelTime: function (val) {
            console.log(val)
        },
        play: function (streamInfo, hasAudio) {
            this.hasAudio = hasAudio;
            this.isLoging = false;
            // this.videoUrl = streamInfo.rtc;
            this.videoUrl = this.getUrlByStreamInfo(streamInfo);
            this.streamId = streamInfo.stream;
            this.app = streamInfo.app;
            this.mediaServerId = streamInfo.mediaServerId;
            this.playFromStreamInfo(false, streamInfo)
        },
        getUrlByStreamInfo(streamInfo){
            let baseZlmApi = process.env.NODE_ENV === 'development'?`${location.host}/debug/zlm`:`${location.host}/zlm`
            // return `${baseZlmApi}/${streamInfo.app}/${streamInfo.streamId}.flv`;
            // return `http://${baseZlmApi}/${streamInfo.app}/${streamInfo.streamId}.flv`;
            if (location.protocol === "https:") {
              if (streamInfo.wss_flv === null) {
                console.error("媒体服务器未配置ssl端口, 使用http端口")
                // this.$message({
                //   showClose: true,
                //   message: '媒体服务器未配置ssl端口, ',
                //   type: 'error'
                // });
                return streamInfo.ws_flv
              }else {
                return streamInfo.wss_flv;
              }

            }else {
              return streamInfo.ws_flv;
            }

        },
        coverPlay: function () {
            var that = this;
            this.coverPlaying = true;
            this.$refs.videoPlayer.pause()
            that.$axios({
                method: 'post',
                url: '/api/play/convert/' + that.streamId
                }).then(function (res) {
                    if (res.data.code == 0) {
                        that.convertKey = res.data.key;
                        setTimeout(()=>{
                            that.isLoging = false;
                            that.playFromStreamInfo(false, res.data.data);
                        }, 2000)
                    } else {
                        that.isLoging = false;
                        that.coverPlaying = false;
                        that.$message({
                            showClose: true,
                            message: '转码失败',
                            type: 'error'
                        });
                    }
                }).catch(function (e) {
                    console.log(e)
                    that.coverPlaying = false;
                    that.$message({
                        showClose: true,
                        message: '播放错误',
                        type: 'error'
                    });
                });
        },
        convertStopClick: function() {
            this.convertStop(()=>{
                this.$refs.videoPlayer.play(this.videoUrl)
            });
        },
        convertStop: function(callback) {
            var that = this;
            that.$refs.videoPlayer.pause()
            this.$axios({
                method: 'post',
                url: '/api/play/convertStop/' + this.convertKey
              }).then(function (res) {
                if (res.data.code == 0) {
                  console.log(res.data.msg)
                }else {
                  console.error(res.data.msg)
                }
                 if (callback )callback();
              }).catch(function (e) {});
            that.coverPlaying = false;
            that.convertKey = "";
            // if (callback )callback();
        },

        playFromStreamInfo: function (realHasAudio, streamInfo) {
          this.showVideoDialog = true;
          this.hasaudio = realHasAudio && this.hasaudio;
          this.$refs.videoPlayer.play(this.getUrlByStreamInfo(streamInfo))
        },
        close: function () {
            console.log('关闭视频');
            if (!!this.$refs.videoPlayer){
              this.$refs.videoPlayer.pause();
            }
            this.videoUrl = '';
            this.coverPlaying = false;
            this.showVideoDialog = false;
            if (this.convertKey != '') {
              this.convertStop();
            }
            this.convertKey = ''
            if (this.recordPlay != '') {
              this.stopPlayRecord();
            }
            this.recordPlay = ''
        },

        copySharedInfo: function (data) {
            console.log('复制内容：' + data);
            this.coverPlaying = false;
            this.tracks = []
            let _this = this;
            this.$copyText(data).then(
                function (e) {
                    _this.$message({
                        showClose: true,
                        message: '复制成功',
                        type: 'success'
                    });
                },
                function (e) {
                    _this.$message({
                        showClose: true,
                        message: '复制失败，请手动复制',
                        type: 'error'
                    });
                }
            );
        },

        queryRecords: function () {
            if (!this.videoHistory.date) {
                return;
            }
            this.recordsLoading = true;
            this.videoHistory.searchHistoryResult = [];
            let that = this;
            var startTime = this.videoHistory.date + " 00:00:00";
            var endTime = this.videoHistory.date + " 23:59:59";
            this.$axios({
                method: 'get',
                url: '/api/gb_record/query/' + this.deviceId + '/' + this.channelId + '?startTime=' + startTime + '&endTime=' + endTime
            }).then(function (res) {
                // 处理时间信息
                that.videoHistory.searchHistoryResult = res.data.recordList;
                that.recordsLoading = false;
            }).catch(function (e) {
                console.log(e.message);
                // that.videoHistory.searchHistoryResult = falsificationData.recordData;
            });

        },
        onTimeChange: function (video) {
            // this.queryRecords()
        },
        playRecord: function (row) {
            let that = this;

            let startTime = row.startTime
            this.recordStartTime = row.startTime
            this.showTimeText =  row.startTime.split(" ")[1]
            let endtime = row.endTime
            this.sliderTime = 0;
            this.seekTime = new Date(endtime).getTime() - new Date(startTime).getTime();
            console.log(this.seekTime)
            if (that.streamId != "") {
                that.stopPlayRecord(function () {
                    that.streamId = "",
                        that.playRecord(row);
                })
            } else {
                this.$axios({
                    method: 'get',
                    url: '/api/playback/start/' + this.deviceId + '/' + this.channelId + '?startTime=' + row.startTime + '&endTime=' +
                        row.endTime
                }).then(function (res) {
                    var streamInfo = res.data;
                    that.app = streamInfo.app;
                    that.streamId = streamInfo.stream;
                    that.mediaServerId = streamInfo.mediaServerId;
                    that.ssrc = streamInfo.ssrc;
                    that.videoUrl = that.getUrlByStreamInfo(streamInfo);
                    that.recordPlay = true;
                });
            }
        },
        stopPlayRecord: function (callback) {
            this.$refs.videoPlayer.pause();
            this.videoUrl = '';
            this.$axios({
                method: 'get',
                url: '/api/playback/stop/' + this.deviceId + "/" + this.channelId + "/" + this.streamId
            }).then(function (res) {
                if (callback) callback()
            });
        },
        downloadRecord: function (row) {
            let that = this;
            if (that.streamId != "") {
                that.stopDownloadRecord(function () {
                    that.streamId = "",
                        that.downloadRecord(row);
                })
            } else {
                this.$axios({
                    method: 'get',
                    url: '/api/download/start/' + this.deviceId + '/' + this.channelId + '?startTime=' + row.startTime + '&endTime=' +
                        row.endTime + '&downloadSpeed=4'
                }).then(function (res) {
                    var streamInfo = res.data;
                    that.app = streamInfo.app;
                    that.streamId = streamInfo.stream;
                    that.mediaServerId = streamInfo.mediaServerId;
                    that.videoUrl = that.getUrlByStreamInfo(streamInfo);
                    that.recordPlay = true;
                });
            }
        },
        stopDownloadRecord: function (callback) {
            this.$refs.videoPlayer.pause();
            this.videoUrl = '';
            this.$axios({
                method: 'get',
                url: '/api/download/stop/' + this.deviceId + "/" + this.channelId+ "/" + this.streamId
            }).then(function (res) {
                if (callback) callback()
            });
        },
        ptzCamera: function (command) {
            console.log('云台控制：' + command);
            let that = this;
            this.$axios({
                method: 'post',
                url: '/api/ptz/control/' + this.deviceId + '/' + this.channelId + '?command=' + command + '&horizonSpeed=' + this.controSpeed + '&verticalSpeed=' + this.controSpeed + '&zoomSpeed=' + this.controSpeed
            }).then(function (res) {});
        },
        //////////////////////播放器事件处理//////////////////////////
        videoError: function (e) {
            console.log("播放器错误：" + JSON.stringify(e));
        },
        presetPosition: function (cmdCode, presetPos) {
            console.log('预置位控制：' + this.presetPos + ' : 0x' + cmdCode.toString(16));
            let that = this;
            this.$axios({
                method: 'post',
                url: '/api/ptz/front_end_command/' + this.deviceId + '/' + this.channelId + '?cmdCode=' + cmdCode + '&parameter1=0&parameter2=' + presetPos + '&combindCode2=0'
            }).then(function (res) {});
        },
        setSpeedOrTime: function (cmdCode, groupNum, parameter) {
            let that = this;
            let parameter2 = parameter % 256;
            let combindCode2 = Math.floor(parameter / 256) * 16;
            console.log('前端控制：0x' + cmdCode.toString(16) + ' 0x' + groupNum.toString(16) + ' 0x' + parameter2.toString(16) + ' 0x' + combindCode2.toString(16));
            this.$axios({
                method: 'post',
                url: '/api/ptz/front_end_command/' + this.deviceId + '/' + this.channelId + '?cmdCode=' + cmdCode + '&parameter1=' + groupNum + '&parameter2=' + parameter2 + '&combindCode2=' + combindCode2
            }).then(function (res) {});
        },
        setCommand: function (cmdCode, groupNum, parameter) {
            let that = this;
            console.log('前端控制：0x' + cmdCode.toString(16) + ' 0x' + groupNum.toString(16) + ' 0x' + parameter.toString(16) + ' 0x0');
            this.$axios({
                method: 'post',
                url: '/api/ptz/front_end_command/' + this.deviceId + '/' + this.channelId + '?cmdCode=' + cmdCode + '&parameter1=' + groupNum + '&parameter2=' + parameter + '&combindCode2=0'
            }).then(function (res) {});
        },
        formatTooltip: function (val) {
            var h = parseInt(val / 60);
            var hStr = h < 10 ? ("0" + h) : h;
            var s = val % 60;
            var sStr = s < 10 ? ("0" + s) : s;
            return h + ":" + sStr;
        },
        timeFormatter: function (row, column, cellValue, index) {
            return cellValue.split(" ")[1];
        },
        mergeTime: function (timeArray) {
            var resultArray = [];
            for (let i = 0; i < timeArray.length; i++) {
                var startTime = new Date(timeArray[i].startTime);
                var endTime = new Date(timeArray[i].endTime);
                if (i == 0) {
                    resultArray[0] = {
                        startTime: startTime,
                        endTime: endTime
                    }
                }
                for (let j = 0; j < resultArray.length; j++) {
                    if (startTime > resultArray[j].endTime) { // 合并
                        if (startTime - resultArray[j].endTime <= 1000) {
                            resultArray[j].endTime = endTime;
                        } else {
                            resultArray[resultArray.length] = {
                                startTime: startTime,
                                endTime: endTime
                            }
                        }
                    } else if (resultArray[j].startTime > endTime) { // 合并
                        if (resultArray[j].startTime - endTime <= 1000) {
                            resultArray[j].startTime = startTime;
                        } else {
                            resultArray[resultArray.length] = {
                                startTime: startTime,
                                endTime: endTime
                            }
                        }
                    }
                }
            }
            console.log(resultArray)
            return resultArray;
        },
        gbPlay(){
          console.log('前端控制：播放');
          this.$axios({
            method: 'get',
            url: '/api/playback/resume/' + this.streamId
          }).then((res)=> {
            this.$refs.videoPlayer.play(this.videoUrl)
          });
        },
        gbPause(){
          console.log('前端控制：暂停');
          this.$axios({
            method: 'get',
            url: '/api/playback/pause/' + this.streamId
          }).then(function (res) {});
        },
        gbScale(command){
          console.log('前端控制：倍速 ' + command);
          this.$axios({
            method: 'get',
            url: `/api/playback/speed/${this.streamId }/${command}`
          }).then(function (res) {});
        },
        gbSeek(val){
          console.log('前端控制：seek ');
          console.log(this.seekTime);
          console.log(this.sliderTime);
          let showTime = new Date(new Date(this.recordStartTime).getTime() + this.seekTime * val / 100)
          let hour = showTime.getHours();
          let minutes = showTime.getMinutes();
          let seconds = showTime.getSeconds();
          this.showTimeText = (hour < 10?("0" + hour):hour) + ":" + (minutes<10?("0" + minutes):minutes) + ":" + (seconds<10?("0" + seconds):seconds)
          this.$axios({
            method: 'get',
            url: `/api/playback/seek/${this.streamId }/` + Math.floor(this.seekTime * val / 100000)
          }).then(function (res) {});
        }

    }
};
</script>

<style>
.control-wrapper {
    position: relative;
    width: 6.25rem;
    height: 6.25rem;
    max-width: 6.25rem;
    max-height: 6.25rem;
    border-radius: 100%;
    margin-top: 1.5rem;
    margin-left: 0.5rem;
    float: left;
}

.control-panel {
    position: relative;
    top: 0;
    left: 5rem;
    height: 11rem;
    max-height: 11rem;
}

.control-btn {
    display: flex;
    justify-content: center;
    position: absolute;
    width: 44%;
    height: 44%;
    border-radius: 5px;
    border: 1px solid #78aee4;
    box-sizing: border-box;
    transition: all 0.3s linear;
}
.control-btn:hover {
    cursor:pointer
}

.control-btn i {
    font-size: 20px;
    color: #78aee4;
    display: flex;
    justify-content: center;
    align-items: center;
}
.control-btn i:hover {
    cursor:pointer
}
.control-zoom-btn:hover {
    cursor:pointer
}

.control-round {
    position: absolute;
    top: 21%;
    left: 21%;
    width: 58%;
    height: 58%;
    background: #fff;
    border-radius: 100%;
}

.control-round-inner {
    position: absolute;
    left: 13%;
    top: 13%;
    display: flex;
    justify-content: center;
    align-items: center;
    width: 70%;
    height: 70%;
    font-size: 40px;
    color: #78aee4;
    border: 1px solid #78aee4;
    border-radius: 100%;
    transition: all 0.3s linear;
}

.control-inner-btn {
    position: absolute;
    width: 60%;
    height: 60%;
    background: #fafafa;
}

.control-top {
    top: -8%;
    left: 27%;
    transform: rotate(-45deg);
    border-radius: 5px 100% 5px 0;
}

.control-top i {
    transform: rotate(45deg);
    border-radius: 5px 100% 5px 0;
}

.control-top .control-inner {
    left: -1px;
    bottom: 0;
    border-top: 1px solid #78aee4;
    border-right: 1px solid #78aee4;
    border-radius: 0 100% 0 0;
}

.control-top .fa {
    transform: rotate(45deg) translateY(-7px);
}

.control-left {
    top: 27%;
    left: -8%;
    transform: rotate(45deg);
    border-radius: 5px 0 5px 100%;
}

.control-left i {
    transform: rotate(-45deg);
}

.control-left .control-inner {
    right: -1px;
    top: -1px;
    border-bottom: 1px solid #78aee4;
    border-left: 1px solid #78aee4;
    border-radius: 0 0 0 100%;
}

.control-left .fa {
    transform: rotate(-45deg) translateX(-7px);
}

.control-right {
    top: 27%;
    right: -8%;
    transform: rotate(45deg);
    border-radius: 5px 100% 5px 0;
}

.control-right i {
    transform: rotate(-45deg);
}

.control-right .control-inner {
    left: -1px;
    bottom: -1px;
    border-top: 1px solid #78aee4;
    border-right: 1px solid #78aee4;
    border-radius: 0 100% 0 0;
}

.control-right .fa {
    transform: rotate(-45deg) translateX(7px);
}

.control-bottom {
    left: 27%;
    bottom: -8%;
    transform: rotate(45deg);
    border-radius: 0 5px 100% 5px;
}

.control-bottom i {
    transform: rotate(-45deg);
}

.control-bottom .control-inner {
    top: -1px;
    left: -1px;
    border-bottom: 1px solid #78aee4;
    border-right: 1px solid #78aee4;
    border-radius: 0 0 100% 0;
}

.control-bottom .fa {
    transform: rotate(-45deg) translateY(7px);
}
.trank {
    width: 80%;
    height: 180px;
    text-align: left;
    padding: 0 10%;
    overflow: auto;
}
.trankInfo {
    width: 80%;
    padding: 0 10%;
}
</style>
