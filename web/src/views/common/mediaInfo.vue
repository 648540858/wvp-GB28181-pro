<template>
  <div id="mediaInfo">
    <el-button style="position: absolute; right: 1rem;" icon="el-icon-refresh-right" circle size="mini" @click="getMediaInfo" />
    <el-descriptions size="mini" :column="3" title="概况">
      <el-descriptions-item label="观看人数">{{ info.readerCount }}</el-descriptions-item>
      <el-descriptions-item label="网络">{{ formatByteSpeed() }}</el-descriptions-item>
      <el-descriptions-item label="持续时间">{{ info.aliveSecond }}秒</el-descriptions-item>
    </el-descriptions>
    <div style="display: grid; grid-template-columns: 1fr 1fr">
      <el-descriptions v-if="info.videoCodec" size="mini" :column="2" title="视频信息">
        <el-descriptions-item label="编码">{{ info.videoCodec }}</el-descriptions-item>
        <el-descriptions-item
          label="分辨率"
        >{{ info.width }}x{{ info.height }}
        </el-descriptions-item>
        <el-descriptions-item label="FPS">{{ info.fps }}</el-descriptions-item>
        <el-descriptions-item label="丢包率">{{ info.loss }}</el-descriptions-item>
      </el-descriptions>
      <el-descriptions v-if="info.audioCodec" size="mini" :column="2" title="音频信息">
        <el-descriptions-item label="编码">
          {{ info.audioCodec }}
        </el-descriptions-item>
        <el-descriptions-item label="采样率">{{ info.audioSampleRate }}</el-descriptions-item>
      </el-descriptions>
    </div>

  </div>
</template>

<script>

export default {
  name: 'MediaInfo',
  components: {},
  props: ['app', 'stream', 'mediaServerId'],
  data() {
    return {
      info: {},
      task: null
    }
  },
  created() {
    this.getMediaInfo()
  },
  methods: {
    getMediaInfo: function() {
      this.$store.dispatch('server/getMediaInfo', {
        app: this.app,
        stream: this.stream,
        mediaServerId: this.mediaServerId
      })
        .then(data => {
          this.info = data
        })
    },
    startTask: function() {
      this.task = setInterval(this.getMediaInfo, 1000)
    },
    stopTask: function() {
      if (this.task) {
        window.clearInterval(this.task)
        this.task = null
      }
    },
    formatByteSpeed: function() {
      const bytesSpeed = this.info.bytesSpeed
      const num = 1024.0 // byte
      if (bytesSpeed < num) return bytesSpeed + ' B/S'
      if (bytesSpeed < Math.pow(num, 2)) return (bytesSpeed / num).toFixed(2) + ' KB/S' // kb
      if (bytesSpeed < Math.pow(num, 3)) { return (bytesSpeed / Math.pow(num, 2)).toFixed(2) + ' MB/S' } // M
      if (bytesSpeed < Math.pow(num, 4)) { return (bytesSpeed / Math.pow(num, 3)).toFixed(2) + ' G/S' } // G
      return (bytesSpeed / Math.pow(num, 4)).toFixed(2) + ' T/S' // T
    },
    formatAliveSecond: function() {
      const aliveSecond = this.info.aliveSecond
      const h = parseInt(aliveSecond.value / 3600)
      const minute = parseInt((aliveSecond.value / 60) % 60)
      const second = Math.ceil(aliveSecond.value % 60)

      const hours = h < 10 ? '0' + h : h
      const formatSecond = second > 59 ? 59 : second
      return `${hours > 0 ? `${hours}小时` : ''}${minute < 10 ? '0' + minute : minute}分${
        formatSecond < 10 ? '0' + formatSecond : formatSecond
      }秒`
    }
  }
}
</script>
<style>
.channel-form {
  display: grid;
  background-color: #FFFFFF;
  padding: 1rem 2rem 0 2rem;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 1rem;
}
</style>
