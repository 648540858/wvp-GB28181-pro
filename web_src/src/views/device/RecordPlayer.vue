<template>
  <a-modal
    title="视频播放"
    :visible="isShowPlayer"
    :width="900"
    :confirmLoading="false"
    :footer="null"
    size="small"
    @cancel="close()">
    <player
      ref="jessibucaPlayer"
      :visible="showVideoDialog"
      :videoUrl="videoUrl"
      :error="videoError"
      :message="videoError"
      :height="false"
      :hasAudio="hasAudio"
      fluent autoplay live>
    </player>
    <p v-if="isRecording" style="margin-top: 1rem;margin-bottom: 0; padding: 0">
      <font-awesome-icon :icon="['fas', 'exclamation-triangle'] " style="color: red;margin-right: 0.25rem"/>
      注意：缓存的是硬盘录像机上的录像，当前流名称：<code style="color: #f5222d">{{streamId}}</code>，
      关闭窗口后请到"云端录像-NVR录像列表"中根据流名称查询，如若查询不到请等1分钟再试。
    </p>
  </a-modal>

</template>

<script>
import player from '@/components/VideoPlayer/jessibuca'
import {downloadRecord, startPlayRecord, stopDownloadRecord, stopPlayRecord} from "@/api/deviceList";

export default {
  name: 'recordPlayer',
  props: {},
  components: {
    player
  },
  created() {
  },
  data() {
    return {
      videoUrl: '',
      isShowPlayer: false,
      showVideoDialog: false,
      hasAudio: false,
      recordRow: null,
      deviceRow: null,
      actionName: '',
      isPlaying: false,
      isRecording: false,
      streamId: ''
    };
  },
  methods: {
    openDialog(recordRow, deviceRow, actionName) {
      this.isShowPlayer = true
      this.recordRow = recordRow
      this.deviceRow = deviceRow
      this.actionName = actionName
      if (!!this.$refs.jessibucaPlayer) {
        this.$refs.jessibucaPlayer.pause();
      }
      if ('play' === actionName) {
        this.playRecord()
      }
      if ('download' === actionName) {
        this.downloadRecord()
      }
    },

    playRecord() {
      if (this.isPlaying) {
        this.stopPlayRecord(() => {
          this.isPlaying = false
          this.showVideoDialog = false
          this.playRecord();
        })
      } else {
        let params = {
          deviceId: this.deviceRow.deviceId,
          channelId: this.deviceRow.channelId,
          startTime: this.recordRow.startTime,
          endTime: this.recordRow.endTime
        }
        startPlayRecord(params).then(streamInfo => {
          this.showVideoDialog = true;
          this.isPlaying = true
          this.hasAudio = streamInfo.hasAudio ? streamInfo.hasAudio : false
          this.videoUrl = this.getUrlByStreamInfo(streamInfo);
        })
      }
    },

    getUrlByStreamInfo(streamInfo) {
      if (location.protocol === "https:") {
        if (streamInfo.wss_flv === null) {
          this.$message({
            showClose: true,
            message: '媒体服务器未配置ssl端口',
            type: 'error'
          });
        } else {
          return streamInfo.wss_flv;
        }

      } else {
        return streamInfo.ws_flv;
      }
    },

    stopPlayRecord(callback) {
      this.videoUrl = '';
      stopPlayRecord({deviceId: this.deviceRow.deviceId, channelId: this.deviceRow.channelId}).then(res => {
        if (callback) callback()
      })
    },

    downloadRecord() {
      if (this.isRecording) {
        this.stopDownloadRecord(() => {
          this.isRecording = false
          this.showVideoDialog = false
          this.downloadRecord()
        })
      }
      let params = {
        deviceId: this.deviceRow.deviceId,
        channelId: this.deviceRow.channelId,
        startTime: this.recordRow.startTime,
        endTime: this.recordRow.endTime,
        downloadSpeed: 4
      }
      downloadRecord(params).then(streamInfo => {
        console.log('streamInfo streamInfo: '+ JSON.stringify(streamInfo))
        this.streamId = streamInfo.streamId
        this.showVideoDialog = true;
        this.hasAudio = streamInfo.hasAudio ? streamInfo.hasAudio : false
        this.isRecording = true
        this.videoUrl = this.getUrlByStreamInfo(streamInfo);
      })
    },

    stopDownloadRecord(callback) {
      this.videoUrl = '';
      let params = {
        deviceId: this.deviceRow.deviceId,
        channelId: this.deviceRow.channelId
      }
      stopDownloadRecord(params).then(res => {
        console.log('stopDownloadRecord: '+JSON.stringify(res))
        if (callback) callback()
      })
    },

    close() {
      console.log('关闭视频');
      if (!!this.$refs.jessibucaPlayer) {
        this.$refs.jessibucaPlayer.pause();
      }
      if (this.isPlaying) {
        this.stopPlayRecord()
      }
      if (this.isRecording) {
        this.stopDownloadRecord()
      }
      this.videoUrl = '';
      this.showVideoDialog = false;
      this.isShowPlayer = false
    },

    videoError: function (e) {
      console.log("播放器错误：" + JSON.stringify(e));
    },
  }
};
</script>

<style>
</style>
