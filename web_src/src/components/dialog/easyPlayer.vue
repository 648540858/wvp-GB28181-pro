<template>
  <div id="easyplayer"></div>
</template>

<script>
export default {
    name: 'player',
    data() {
        return {
            easyPlayer: null
        };
    },
    props: ['videoUrl', 'error', 'hasaudio', 'height'],
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
            this.play(newData)
        },
        immediate:true
    },
    methods: {
        play: function (url) {
          console.log(this.height)
            if (this.easyPlayer != null) {
              this.easyPlayer.destroy();
            }
            if (typeof (this.height) == "undefined") {
              this.height = false
            }
            this.easyPlayer = new WasmPlayer(null, 'easyplayer', this.eventcallbacK, {Height: this.height})
            this.easyPlayer.play(url, 1)
        },
        pause: function () {
          this.easyPlayer.destroy();
          this.easyPlayer = null
        },
        eventcallbacK: function(type, message) {
            // console.log("player 事件回调")
            // console.log(type)
            // console.log(message)
        }
    },
    destroyed() {
      this.easyPlayer.destroy();
    },
}
</script>

<style>
    .LodingTitle {
        min-width: 70px;
    }
    /* 隐藏logo */
    .iconqingxiLOGO {
        display: none !important;
    }

</style>
