<template>
  <a-modal
    title="视频播放"
    :visible="isShowPlayer"
    :width="900"
    :footer="null"
    @cancel="close()">
    <div :style="{'height': height+'px'}">
      <easy-player ref="easyPlayer" :video-url="videoUrl" :height="true" :hasaudio="hasAudio"/>
    </div>
  </a-modal>
</template>

<script>
import easyPlayer from "@/components/VideoPlayer/easyPlayer";

export default {
  name: "PlayerDialog",
  components: {
    easyPlayer
  },
  props: {
    height: {
      type: Number,
      default: 500
    }
  },
  data() {
    return {
      hasAudio: false,
      videoUrl: '',
      isShowPlayer: false
    }
  },
  methods: {
    play(url, hasAudio) {
      this.isShowPlayer = true
      this.hasAudio = hasAudio
      this.videoUrl = url
    },
    close() {
      this.isShowPlayer = false
    }
  }
}
</script>

<style scoped>

</style>