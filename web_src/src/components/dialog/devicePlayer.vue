<template>
<div id="devicePlayer" v-loading="isLoging">

    <el-dialog title="视频播放" top="0" :close-on-click-modal="false" :visible.sync="showVideoDialog" @close="close()">
      <div style="width: 100%; height: 100%">
        <el-tabs type="card" :stretch="true" v-model="activePlayer" @tab-click="changePlayer" v-if="Object.keys(this.player).length > 1">
<!--          <el-tab-pane label="LivePlayer" name="livePlayer">-->
<!--            <LivePlayer v-if="showVideoDialog" ref="livePlayer" :visible.sync="showVideoDialog" :videoUrl="videoUrl" :error="videoError" :message="videoError" :hasaudio="hasAudio" fluent autoplay live></LivePlayer>-->
<!--          </el-tab-pane>-->
          <el-tab-pane label="Jessibuca" name="jessibuca">
            <jessibucaPlayer v-if="activePlayer === 'jessibuca'" ref="jessibuca" :visible.sync="showVideoDialog" :videoUrl="videoUrl" :error="videoError" :message="videoError" height="100px" :hasAudio="hasAudio" fluent autoplay live ></jessibucaPlayer>
          </el-tab-pane>
          <el-tab-pane label="WebRTC" name="webRTC">
            <rtc-player v-if="activePlayer === 'webRTC'" ref="webRTC" :visible.sync="showVideoDialog" :videoUrl="videoUrl" :error="videoError" :message="videoError" height="100px" :hasAudio="hasAudio" fluent autoplay live ></rtc-player>
          </el-tab-pane>
          <el-tab-pane label="h265web">h265web敬请期待</el-tab-pane>
        </el-tabs>
        <jessibucaPlayer v-if="Object.keys(this.player).length == 1 && this.player.jessibuca" ref="jessibuca" :visible.sync="showVideoDialog" :videoUrl="videoUrl" :error="videoError" :message="videoError" height="100px" :hasAudio="hasAudio" fluent autoplay live ></jessibucaPlayer>
        <rtc-player v-if="Object.keys(this.player).length == 1 && this.player.webRTC" ref="jessibuca" :visible.sync="showVideoDialog" :videoUrl="videoUrl" :error="videoError" :message="videoError" height="100px" :hasAudio="hasAudio" fluent autoplay live ></rtc-player>

      </div>
        <div id="shared" style="text-align: right; margin-top: 1rem;">
            <el-tabs v-model="tabActiveName" @tab-click="tabHandleClick" >
                <el-tab-pane label="实时视频" name="media">
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
                          <el-button slot="append" icon="el-icon-document-copy" title="点击拷贝" v-clipboard="getPlayerShared.sharedRtmp" @success="$message({type:'success', message:'成功拷贝到粘贴板'})"></el-button>
                            <el-dropdown slot="prepend" v-if="streamInfo" trigger="click" @command="copyUrl">
                              <el-button >
                                更多地址<i class="el-icon-arrow-down el-icon--right"></i>
                              </el-button>
                              <el-dropdown-menu slot="dropdown" >
                                <el-dropdown-item v-if="streamInfo.flv" :command="streamInfo.flv">
                                  <el-tag >FLV:</el-tag>
                                  <span>{{ streamInfo.flv }}</span>
                                </el-dropdown-item>
                                <el-dropdown-item v-if="streamInfo.https_flv" :command="streamInfo.https_flv">
                                  <el-tag >FLV(https):</el-tag>
                                  <span>{{ streamInfo.https_flv }}</span>
                                </el-dropdown-item>
                                <el-dropdown-item v-if="streamInfo.ws_flv" :command="streamInfo.ws_flv">
                                  <el-tag  >FLV(ws):</el-tag>
                                  <span >{{ streamInfo.ws_flv }}</span>
                                </el-dropdown-item>
                                <el-dropdown-item v-if="streamInfo.wss_flv" :command="streamInfo.wss_flv">
                                  <el-tag  >FLV(wss):</el-tag>
                                  <span>{{ streamInfo.wss_flv }}</span>
                                </el-dropdown-item>
                                <el-dropdown-item v-if="streamInfo.fmp4" :command="streamInfo.fmp4">
                                  <el-tag >FMP4:</el-tag>
                                  <span>{{ streamInfo.fmp4 }}</span>
                                </el-dropdown-item>
                                <el-dropdown-item v-if="streamInfo.https_fmp4" :command="streamInfo.https_fmp4">
                                  <el-tag >FMP4(https):</el-tag>
                                  <span>{{ streamInfo.https_fmp4 }}</span>
                                </el-dropdown-item>
                                <el-dropdown-item v-if="streamInfo.ws_fmp4" :command="streamInfo.ws_fmp4">
                                  <el-tag >FMP4(ws):</el-tag>
                                  <span>{{ streamInfo.ws_fmp4 }}</span>
                                </el-dropdown-item>
                                <el-dropdown-item v-if="streamInfo.wss_fmp4" :command="streamInfo.wss_fmp4">
                                  <el-tag >FMP4(wss):</el-tag>
                                  <span>{{ streamInfo.wss_fmp4 }}</span>
                                </el-dropdown-item>
                                <el-dropdown-item v-if="streamInfo.hls" :command="streamInfo.hls">
                                  <el-tag>HLS:</el-tag>
                                  <span>{{ streamInfo.hls }}</span>
                                </el-dropdown-item>
                                <el-dropdown-item v-if="streamInfo.https_hls" :command="streamInfo.https_hls">
                                  <el-tag >HLS(https):</el-tag>
                                  <span>{{ streamInfo.https_hls }}</span>
                                </el-dropdown-item>
                                <el-dropdown-item v-if="streamInfo.ws_hls" :command="streamInfo.ws_hls">
                                  <el-tag >HLS(ws):</el-tag>
                                  <span>{{ streamInfo.ws_hls }}</span>
                                </el-dropdown-item>
                                <el-dropdown-item v-if="streamInfo.wss_hls"  :command="streamInfo.wss_hls">
                                  <el-tag >HLS(wss):</el-tag>
                                  <span>{{ streamInfo.wss_hls }}</span>
                                </el-dropdown-item>
                                <el-dropdown-item v-if="streamInfo.ts"  :command="streamInfo.ts">
                                  <el-tag>TS:</el-tag>
                                  <span>{{ streamInfo.ts }}</span>
                                </el-dropdown-item>
                                <el-dropdown-item v-if="streamInfo.https_ts" :command="streamInfo.https_ts">
                                  <el-tag>TS(https):</el-tag>
                                  <span>{{ streamInfo.https_ts }}</span>
                                </el-dropdown-item>
                                <el-dropdown-item v-if="streamInfo.ws_ts" :command="streamInfo.ws_ts">
                                  <el-tag>TS(ws):</el-tag>
                                  <span>{{ streamInfo.ws_ts }}</span>
                                </el-dropdown-item>
                                <el-dropdown-item v-if="streamInfo.wss_ts" :command="streamInfo.wss_ts">
                                  <el-tag>TS(wss):</el-tag>
                                  <span>{{ streamInfo.wss_ts }}</span>
                                </el-dropdown-item>
                                <el-dropdown-item v-if="streamInfo.rtc" :command="streamInfo.rtc">
                                  <el-tag >RTC:</el-tag>
                                  <span>{{ streamInfo.rtc }}</span>
                                </el-dropdown-item>
                                <el-dropdown-item v-if="streamInfo.rtcs" :command="streamInfo.rtcs">
                                  <el-tag >RTCS:</el-tag>
                                  <span>{{ streamInfo.rtcs }}</span>
                                </el-dropdown-item>
                                <el-dropdown-item v-if="streamInfo.rtmp" :command="streamInfo.rtmp">
                                  <el-tag >RTMP:</el-tag>
                                  <span>{{ streamInfo.rtmp }}</span>
                                </el-dropdown-item>
                                <el-dropdown-item v-if="streamInfo.rtmps" :command="streamInfo.rtmps">
                                  <el-tag >RTMPS:</el-tag>
                                  <span>{{ streamInfo.rtmps }}</span>
                                </el-dropdown-item>
                                <el-dropdown-item v-if="streamInfo.rtsp" :command="streamInfo.rtsp">
                                  <el-tag >RTSP:</el-tag>
                                  <span>{{ streamInfo.rtsp }}</span>
                                </el-dropdown-item>
                                <el-dropdown-item v-if="streamInfo.rtsps" :command="streamInfo.rtsps">
                                  <el-tag >RTSPS:</el-tag>
                                  <span>{{ streamInfo.rtsps }}</span>
                                </el-dropdown-item>
                              </el-dropdown-menu>
                            </el-dropdown>
                        </el-input>

                    </div>
                </el-tab-pane>
                <!--{"code":0,"data":{"paths":["22-29-30.mp4"],"rootPath":"/home/kkkkk/Documents/ZLMediaKit/release/linux/Debug/www/record/hls/kkkkk/2020-05-11/"}}-->
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
                                <el-tag style="position :absolute; left: 0rem; top: 0rem; width: 5rem; text-align: center" size="medium">预置位编号</el-tag>
                                <el-input-number style="position: absolute; left: 5rem; top: 0rem; width: 6rem" size="mini" v-model="presetPos" controls-position="right" :precision="0" :step="1" :min="1" :max="255"></el-input-number>
                                <el-button style="position: absolute; left: 11rem; top: 0rem; width: 5rem" size="mini" icon="el-icon-add-location" @click="presetPosition(129, presetPos)">设置</el-button>
                                <el-button style="position: absolute; left: 27rem; top: 0rem; width: 5rem" size="mini" type="primary" icon="el-icon-place" @click="presetPosition(130, presetPos)">调用</el-button>
                                <el-button style="position: absolute; left: 16rem; top: 0rem; width: 5rem" size="mini" icon="el-icon-delete-location" @click="presetPosition(131, presetPos)">删除</el-button>
                                <el-tag style="position :absolute; left: 0rem; top: 2.5rem; width: 5rem; text-align: center" size="medium">巡航速度</el-tag>
                                <el-input-number style="position: absolute; left: 5rem; top: 2.5rem; width: 6rem" size="mini" v-model="cruisingSpeed" controls-position="right" :precision="0" :min="1" :max="4095"></el-input-number>
                                <el-button style="position: absolute; left: 11rem; top: 2.5rem; width: 5rem" size="mini" icon="el-icon-loading" @click="setSpeedOrTime(134, cruisingGroup, cruisingSpeed)">设置</el-button>
                                <el-tag style="position :absolute; left: 16rem; top: 2.5rem; width: 5rem; text-align: center" size="medium">停留时间</el-tag>
                                <el-input-number style="position: absolute; left: 21rem; top: 2.5rem; width: 6rem" size="mini" v-model="cruisingTime" controls-position="right" :precision="0" :min="1" :max="4095"></el-input-number>
                                <el-button style="position: absolute; left: 27rem; top: 2.5rem; width: 5rem" size="mini" icon="el-icon-timer" @click="setSpeedOrTime(135, cruisingGroup, cruisingTime)">设置</el-button>
                                <el-tag style="position :absolute; left: 0rem; top: 4.5rem; width: 5rem; text-align: center" size="medium">巡航组编号</el-tag>
                                <el-input-number style="position: absolute; left: 5rem; top: 4.5rem; width: 6rem" size="mini" v-model="cruisingGroup" controls-position="right" :precision="0" :min="0" :max="255"></el-input-number>
                                <el-button style="position: absolute; left: 11rem; top: 4.5rem; width: 5rem" size="mini" icon="el-icon-add-location" @click="setCommand(132, cruisingGroup, presetPos)">添加点</el-button>
                                <el-button style="position: absolute; left: 16rem; top: 4.5rem; width: 5rem" size="mini" icon="el-icon-delete-location" @click="setCommand(133, cruisingGroup, presetPos)">删除点</el-button>
                                <el-button style="position: absolute; left: 21rem; top: 4.5rem; width: 5rem" size="mini" icon="el-icon-delete" @click="setCommand(133, cruisingGroup, 0)">删除组</el-button>
                                <el-button style="position: absolute; left: 27rem; top: 5rem; width: 5rem" size="mini" type="primary" icon="el-icon-video-camera-solid" @click="setCommand(136, cruisingGroup, 0)">巡航</el-button>
                                <el-tag style="position :absolute; left: 0rem; top: 7rem; width: 5rem; text-align: center" size="medium">扫描速度</el-tag>
                                <el-input-number style="position: absolute; left: 5rem; top: 7rem; width: 6rem" size="mini" v-model="scanSpeed" controls-position="right" :precision="0" :min="1" :max="4095"></el-input-number>
                                <el-button style="position: absolute; left: 11rem; top: 7rem; width: 5rem" size="mini" icon="el-icon-loading" @click="setSpeedOrTime(138, scanGroup, scanSpeed)">设置</el-button>
                                <el-tag style="position :absolute; left: 0rem; top: 9rem; width: 5rem; text-align: center" size="medium">扫描组编号</el-tag>
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
import rtcPlayer from '../dialog/rtcPlayer.vue'
import LivePlayer from '@liveqing/liveplayer'
import jessibucaPlayer from '../common/jessibuca.vue'
export default {
    name: 'devicePlayer',
    props: {},
    components: {
      LivePlayer, jessibucaPlayer, rtcPlayer,
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
    created() {
      console.log(this.player)
      if (Object.keys(this.player).length === 1) {
        this.activePlayer = Object.keys(this.player)[0]
      }
    },
    data() {
        return {
            video: 'http://lndxyj.iqilu.com/public/upload/2019/10/14/8c001ea0c09cdc59a57829dabc8010fa.mp4',
            videoUrl: '',
            activePlayer: "jessibuca",
            // 如何你只是用一种播放器，直接注释掉不用的部分即可
            player: {
              jessibuca : ["ws_flv", "wss_flv"],
              livePlayer : ["ws_flv", "wss_flv"],
              webRTC: ["rtc", "rtcs"],
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
            showPtz: true,
            showRrecord: true,
            tracksNotLoaded: false,
            sliderTime: 0,
            seekTime: 0,
            recordStartTime: 0,
            showTimeText: "00:00:00",
            streamInfo: null,
        };
    },
    methods: {
        tabHandleClick: function(tab, event) {
            console.log(tab)
            var that = this;
            that.tracks = [];
            that.tracksLoading = true;
            that.tracksNotLoaded = false;
            if (tab.name === "codec") {
                this.$axios({
                    method: 'get',
                    url: '/zlm/' +this.mediaServerId+ '/index/api/getMediaInfo?vhost=__defaultVhost__&schema=rtsp&app='+ this.app +'&stream='+ this.streamId
                }).then(function (res) {
                    that.tracksLoading = false;
                    if (res.data.code == 0 && res.data.tracks) {
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
        changePlayer: function (tab) {
            console.log(this.player[tab.name][0])
            this.activePlayer = tab.name;
            this.videoUrl = this.getUrlByStreamInfo()
            console.log(this.videoUrl)
        },
        openDialog: function (tab, deviceId, channelId, param) {
            if (this.showVideoDialog) {
              return;
            }
            this.tabActiveName = tab;
            this.channelId = channelId;
            this.deviceId = deviceId;
            this.streamId = "";
            this.mediaServerId = "";
            this.app = "";
            this.videoUrl = ""
            if (!!this.$refs[this.activePlayer]) {
              this.$refs[this.activePlayer].pause();
            }
            switch (tab) {
                case "media":
                    this.play(param.streamInfo, param.hasAudio)
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
        play: function (streamInfo, hasAudio) {
            this.streamInfo = streamInfo;
            this.hasAudio = hasAudio;
            this.isLoging = false;
            // this.videoUrl = streamInfo.rtc;
            this.videoUrl = this.getUrlByStreamInfo();
            this.streamId = streamInfo.stream;
            this.app = streamInfo.app;
            this.mediaServerId = streamInfo.mediaServerId;
            this.playFromStreamInfo(false, streamInfo)
        },
        getUrlByStreamInfo(){
            console.log(this.streamInfo)
            if (location.protocol === "https:") {
              this.videoUrl = this.streamInfo[this.player[this.activePlayer][1]]
            }else {
              this.videoUrl = this.streamInfo[this.player[this.activePlayer][0]]
            }
            return this.videoUrl;

        },
        coverPlay: function () {
            var that = this;
            this.coverPlaying = true;
            this.$refs[this.activePlayer].pause()
            that.$axios({
                method: 'post',
                url: '/api/play/convert/' + that.streamId
                }).then(function (res) {
                    if (res.data.code === 0) {
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
                this.$refs[this.activePlayer].play(this.videoUrl)
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
          if (this.$refs[this.activePlayer]) {
            this.$refs[this.activePlayer].play(this.getUrlByStreamInfo(streamInfo))
          }else {
            this.$nextTick(() => {
              this.$refs[this.activePlayer].play(this.getUrlByStreamInfo(streamInfo))
            });
          }


        },
        close: function () {
            console.log('关闭视频');
            if (!!this.$refs[this.activePlayer]){
              this.$refs[this.activePlayer].pause();
            }
            this.videoUrl = '';
            this.coverPlaying = false;
            this.showVideoDialog = false;
            if (this.convertKey != '') {
              this.convertStop();
            }
            this.convertKey = ''
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
        copyUrl: function (dropdownItem){
            console.log(dropdownItem)
            this.$copyText(dropdownItem).then((e)=> {
              this.$message.success("成功拷贝到粘贴板");
            }, (e)=> {

            })
        },



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
