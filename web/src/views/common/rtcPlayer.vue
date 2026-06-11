<template>
  <div id="rtcPlayer">
    <video id="webRtcPlayerBox" :controls="showControls" autoplay style="text-align:left;">
      Your browser is too old which doesn't support HTML5 video.
    </video>
  </div>
</template>

<script>
let webrtcPlayer = null
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
  },
  methods: {
    play: function(url) {
      if (webrtcPlayer != null) {
        this.pause()
      }
      webrtcPlayer = new ZLMRTCClient.Endpoint({
        element: document.getElementById('webRtcPlayerBox'), // video 标签
        debug: true, // 是否打印日志
        zlmsdpUrl: url, // 流地址
        simulecast: false,
        useCamera: false,
        audioEnable: true,
        videoEnable: true,
        recvOnly: true,
        usedatachannel: false
      })
      webrtcPlayer.on(ZLMRTCClient.Events.WEBRTC_ICE_CANDIDATE_ERROR, (e) => { // ICE 协商出错
        console.error('ICE 协商出错')
        this.eventcallbacK('ICE ERROR', 'ICE 协商出错')
      })

      webrtcPlayer.on(ZLMRTCClient.Events.WEBRTC_ON_REMOTE_STREAMS, (e) => { // 获取到了远端流，可以播放
        console.log('播放成功', e.streams)
        this.eventcallbacK('playing', '播放成功')
      })

      webrtcPlayer.on(ZLMRTCClient.Events.WEBRTC_OFFER_ANWSER_EXCHANGE_FAILED, (e) => { // offer anwser 交换失败
        console.error('offer anwser 交换失败', e)
        this.eventcallbacK('OFFER ANSWER ERROR ', 'offer anwser 交换失败')
        if (e.code == -400 && e.msg == '流不存在') {
          console.log('流不存在')
          this.timer = setTimeout(() => {
            this.webrtcPlayer.close()
            this.play(url)
          }, 100)
        }
      })

      webrtcPlayer.on(ZLMRTCClient.Events.WEBRTC_ON_LOCAL_STREAM, (s) => { // 获取到了本地流
        // document.getElementById('selfVideo').srcObject=s;
        this.eventcallbacK('LOCAL STREAM', '获取到了本地流')
      })
    },
    pause: function() {
      if (webrtcPlayer != null) {
        webrtcPlayer.close()
        webrtcPlayer = null
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
      return document.getElementById('webRtcPlayerBox')
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
    #rtcPlayer{
        width: 100%;
        height: 100%;
        position: relative;
    }
    #webRtcPlayerBox{
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
