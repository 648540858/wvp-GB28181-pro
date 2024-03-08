<template>
    <div id="rtcPlayer">
        <video id='webRtcPlayerBox' controls autoplay style="text-align:left;">
            Your browser is too old which doesn't support HTML5 video.
          </video>
    </div>
</template>

<script>
let webrtcPlayer = null;
export default {
    name: 'rtcPlayer',
    data() {
        return {
            timer: null
        };
    },
    props: ['videoUrl', 'error', 'hasaudio'],
    mounted () {
      let paramUrl = decodeURIComponent(this.$route.params.url)
       this.$nextTick(() =>{
         if (typeof (this.videoUrl) == "undefined") {
           this.videoUrl = paramUrl;
         }
         console.log("初始化时的地址为: " + this.videoUrl)
         this.play(this.videoUrl)
        })
    },
    watch:{
        videoUrl(newData, oldData){
            this.pause();
            this.play(newData);
        },
        immediate:true
    },
    methods: {
        play: function (url) {
            webrtcPlayer = new ZLMRTCClient.Endpoint({
                element: document.getElementById('webRtcPlayerBox'),// video 标签
                debug: true,// 是否打印日志
                zlmsdpUrl: url,//流地址
                simulecast: false,
                useCamera: false,
                audioEnable: false,
                videoEnable: false,
                recvOnly: true,
            })
            webrtcPlayer.on(ZLMRTCClient.Events.WEBRTC_ICE_CANDIDATE_ERROR,(e)=>{// ICE 协商出错
                console.error('ICE 协商出错')
                this.eventcallbacK("ICE ERROR", "ICE 协商出错")
            });

            webrtcPlayer.on(ZLMRTCClient.Events.WEBRTC_ON_REMOTE_STREAMS,(e)=>{//获取到了远端流，可以播放
                console.log('播放成功',e.streams)
                this.eventcallbacK("playing", "播放成功")
            });

            webrtcPlayer.on(ZLMRTCClient.Events.WEBRTC_OFFER_ANWSER_EXCHANGE_FAILED,(e)=>{// offer anwser 交换失败
                console.error('offer anwser 交换失败',e)
                this.eventcallbacK("OFFER ANSWER ERROR ", "offer anwser 交换失败")
                if (e.code ==-400 && e.msg=="流不存在"){
                    console.log("流不存在")
                    this.timer = setTimeout(()=>{
                        this.webrtcPlayer.close();
                        this.play(url)
                    }, 100)

                }
            });

            webrtcPlayer.on(ZLMRTCClient.Events.WEBRTC_ON_LOCAL_STREAM,(s)=>{// 获取到了本地流

                // document.getElementById('selfVideo').srcObject=s;
                this.eventcallbacK("LOCAL STREAM", "获取到了本地流")
            });

        },
        pause: function () {
            if (webrtcPlayer != null) {
                webrtcPlayer.close();
                webrtcPlayer = null;
            }

        },
        eventcallbacK: function(type, message) {
            console.log("player 事件回调")
            console.log(type)
            console.log(message)
        }
    },
    destroyed() {
        clearTimeout(this.timer);
    },
}
</script>

<style>
    .LodingTitle {
        min-width: 70px;
    }
    #rtcPlayer{
        width: 100%;
    }
    #webRtcPlayerBox{
        width: 100%;
        max-height: 56vh;
        background-color: #000;
    }
    /* 隐藏logo */
    /* .iconqingxiLOGO {
        display: none !important;
    } */

</style>
