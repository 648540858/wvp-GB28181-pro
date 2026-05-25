<template>
  <div style="width: 100vw; height: 100vh; background-color: #000; overflow: hidden;">
    <jessibucaPlayer
      v-if="playerType === 0"
      ref="player"
      :show-button="true"
      style="width: 100%; height: 100%"
    />
    <rtc-player
      v-if="playerType === 1"
      ref="player"
      :show-controls="true"
      style="width: 100%; height: 100%"
    />
    <h265web
      v-if="playerType === 2"
      ref="player"
      :show-button="true"
      style="width: 100%; height: 100%"
    />
  </div>
</template>

<script>
import jessibucaPlayer from './jessibuca.vue'
import rtcPlayer from './rtcPlayer.vue'
import h265web from './h265web.vue'

export default {
  name: 'SharePlayer',
  components: { jessibucaPlayer, rtcPlayer, h265web },
  data() {
    return {
      playerType: 0
    }
  },
  created() {
    const type = parseInt(this.$route.query.type)
    if (!isNaN(type) && type >= 0 && type <= 2) {
      this.playerType = type
    }
  },
  mounted() {
    const url = this.$route.query.url
    if (url) {
      this.$nextTick(() => {
        if (this.$refs.player) {
          this.$refs.player.play(decodeURIComponent(url))
        }
      })
    }
  }
}
</script>
