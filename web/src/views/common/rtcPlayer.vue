<template>
  <div :id="'rtcPlayer-' + _uid" class="rtc-player-wrapper">
    <video :id="'webRtcPlayerBox-' + _uid" class="rtc-player-video" :controls="showControls" autoplay style="text-align:left;">
      Your browser is too old which doesn't support HTML5 video.
    </video>
  </div>
</template>

<script>
const webrtcPlayer = {}
import dragZoom from '../../mixins/dragZoom'
export default {
  name: 'RtcPlayer',
  mixins: [dragZoom],
  props: {
    videoUrl: { type: String, default: '' },
    error: { default: '' },
    hasaudio: { type: Boolean, default: false },
    showControls: { type: Boolean, default: true }
  },
  data() {
    return {
      timer: null
    }
  },
  mounted() {},
  destroyed() {
    clearTimeout(this.timer)
    this.pause()
  },
  methods: {
    play: function(url) {
      if (webrtcPlayer[this._uid]) {
        this.pause()
      }
      webrtcPlayer[this._uid] = new ZLMRTCClient.Endpoint({
        element: document.getElementById('webRtcPlayerBox-' + this._uid),
        debug: true,
        zlmsdpUrl: url,
        simulecast: false,
        useCamera: false,
        audioEnable: true,
        videoEnable: true,
        recvOnly: true,
        usedatachannel: false
      })
      const player = webrtcPlayer[this._uid]
      player.on(ZLMRTCClient.Events.WEBRTC_ICE_CANDIDATE_ERROR, (e) => {
        console.error('ICE 协商出错')
        this.eventcallbacK('ICE ERROR', 'ICE 协商出错')
      })

      player.on(ZLMRTCClient.Events.WEBRTC_ON_REMOTE_STREAMS, (e) => {
        console.log('播放成功', e.streams)
        this.eventcallbacK('playing', '播放成功')
      })

      player.on(ZLMRTCClient.Events.WEBRTC_OFFER_ANWSER_EXCHANGE_FAILED, (e) => {
        console.error('offer anwser 交换失败', e)
        this.eventcallbacK('OFFER ANSWER ERROR ', 'offer anwser 交换失败')
        if (e.code == -400 && e.msg == '流不存在') {
          console.log('流不存在')
          this.timer = setTimeout(() => {
            player.close()
            this.play(url)
          }, 100)
        }
      })

      player.on(ZLMRTCClient.Events.WEBRTC_ON_LOCAL_STREAM, (s) => {
        this.eventcallbacK('LOCAL STREAM', '获取到了本地流')
      })
    },
    pause: function() {
      if (webrtcPlayer[this._uid]) {
        webrtcPlayer[this._uid].close()
        webrtcPlayer[this._uid] = null
      }
    },
    stop: function() {
      this.pause()
    },
    eventcallbacK: function(type, message) {
      console.log('player 事件回调')
      console.log(type)
      console.log(message)
    },
    getVideoElement() {
      return document.getElementById('webRtcPlayerBox-' + this._uid)
    },
    getVideoRect() {
      const video = this.getVideoElement()
      const rect = video.getBoundingClientRect()
      if (video.videoWidth && video.videoHeight) {
        const natRatio = video.videoWidth / video.videoHeight
        const disRatio = rect.width / rect.height
        let w, h, x, y
        if (natRatio > disRatio) {
          w = rect.width
          h = w / natRatio
          x = 0
          y = (rect.height - h) / 2
        } else {
          h = rect.height
          w = h * natRatio
          x = (rect.width - w) / 2
          y = 0
        }
        return {
          left: rect.left + x, top: rect.top + y,
          right: rect.left + x + w, bottom: rect.top + y + h,
          width: w, height: h
        }
      }
      return rect
    }
  }
}
</script>

<style>
    .LodingTitle {
        min-width: 70px;
    }
    .rtc-player-wrapper{
        width: 100%;
        height: 100%;
        position: relative;
    }
    .rtc-player-video{
        width: 100%;
        height: 100%;
        max-height: 100%;
        background-color: #000;
    }
    /* 隐藏logo */
    /* .iconqingxiLOGO {
        display: none !important;
    } */

</style>
