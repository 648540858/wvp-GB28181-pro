<template>
  <div class="player-tabs-wrapper" ref="playerWrapper">
    <el-tabs v-if="showTab && playerCount > 1" v-model="activePlayer" type="card" :stretch="true" @tab-click="changePlayer">
      <el-tab-pane label="Jessibuca" name="jessibuca"></el-tab-pane>
      <el-tab-pane label="WebRTC" name="webRTC"></el-tab-pane>
      <el-tab-pane label="h265web" name="h265web"></el-tab-pane>
    </el-tabs>
    <div class="player-video-area">
      <jessibucaPlayer
        v-if="activePlayer === 'jessibuca'"
        ref="jessibuca"
        style="width: 100%; height: 100%;"
        :has-audio="hasAudio"
        :show-button="showButton"
        fluent autoplay live
      />
      <rtc-player
        v-if="activePlayer === 'webRTC'"
        ref="webRTC"
        style="width: 100%; height: 100%;"
        :has-audio="hasAudio"
        :show-button="showButton"
        fluent autoplay live
      />
      <h265web
        v-if="activePlayer === 'h265web'"
        ref="h265web"
        style="width: 100%; height: 100%;"
        :has-audio="hasAudio"
        :show-button="showButton"
        fluent autoplay live
      />
    </div>
  </div>
</template>

<script>
import jessibucaPlayer from './jessibuca.vue'
import rtcPlayer from './rtcPlayer.vue'
import h265web from './h265web.vue'

export default {
  name: 'PlayerTabs',
  components: { jessibucaPlayer, rtcPlayer, h265web },
  props: {
    hasAudio: { type: Boolean, default: false },
    showButton: { type: Boolean, default: true }
  },
  data() {
    return {
      showTab: true,
      streamInfo: null,
      activePlayer: 'jessibuca',
      player: { jessibuca: ['ws_flv', 'wss_flv'], webRTC: ['rtc', 'rtcs'], h265web: ['ws_flv', 'wss_flv'] }
    }
  },
  computed: {
    playerCount() {
      return Object.keys(this.player).length
    }
  },
  created() {
    if (this.playerCount === 1) {
      this.activePlayer = Object.keys(this.player)[0]
    }
  },
  methods: {
    getUrlByStreamInfo() {
      if (!this.streamInfo) return ''
      const src = this.streamInfo.transcodeStream || this.streamInfo
      if (location.protocol === 'https:') {
        return src[this.player[this.activePlayer][1]]
      }
      return src[this.player[this.activePlayer][0]]
    },
    changePlayer(tab) {
      this.activePlayer = tab.name
      this.play()
    },
    setStreamInfo(streamInfo) {
      this.streamInfo = streamInfo
      this.play()
    },
    play() {
      let playUrl = this.getUrlByStreamInfo()
      this.$nextTick(() => {
        if (this.$refs[this.activePlayer]) {
          this.$refs[this.activePlayer].play(playUrl)
        }
      })
      const typeMap = { jessibuca: 0, webRTC: 1, h265web: 2 }
      const type = typeMap[this.activePlayer] || 0
      const playerUrl = window.location.origin + '/#/play/share?type=' + type + '&url=' + encodeURIComponent(playUrl)
      this.$emit('playerChanged', { playUrl, playerUrl })
    },
    stop() {
      if (this.$refs[this.activePlayer]) {
        this.$refs[this.activePlayer].pause()
      }
    },
    pause() {
      if (this.$refs[this.activePlayer]) {
        this.$refs[this.activePlayer].pause()
      }
    },
    getVideoRect() {
      const player = this.$refs[this.activePlayer]
      return player && player.getVideoRect ? player.getVideoRect() : null
    },
    startDragZoom(callback) {
      const player = this.$refs[this.activePlayer]
      if (player && player.startDragZoom) {
        player.startDragZoom(callback)
      }
    }
  }
}
</script>

<style scoped>
.player-tabs-wrapper {
  width: 100%;
  height: 100%;
}
.player-tabs-wrapper .el-tabs {
  margin-bottom: 0;
}
.player-tabs-wrapper .el-tabs >>> .el-tabs__header {
  margin-bottom: 0;
}
.player-video-area {
  width: 100%;
  height:calc(100% - 36px);;
  background: #000;
}
</style>
