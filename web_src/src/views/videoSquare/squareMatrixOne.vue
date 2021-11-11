<template>
  <div ref="container" style="background-color: #000;width: 50%;margin-left: 25%;margin-top: 5%"
       @click="selectContainer" @mouseleave="mouseLeave()"></div>

</template>

<script>
import Jessibuca from '@/core/jessibuca/renderer'

export default {
  name: "squareMatrixOne",
  components: {},
  props: ['fullScreen'],
  data() {
    return {
      playerCommonOptions: {},
      jessibuca: null
    }
  },
  created() {
    this.initCommonOptions()
  },
  mounted() {
    this.$emit('setFullScreenEle', this.$refs.container)
    let options = Object.assign({container: this.$refs.container}, this.playerCommonOptions);
    let tempDom = this.$refs.container
    tempDom.style.height = (9 / 16) * tempDom.clientWidth + "px"
    this.jessibuca = new Jessibuca(options)
    this.initPlayerEvent()
    //传递全屏元素
    this.$emit('setFullScreenEle', this.$refs.container, [tempDom], {player: this.jessibuca})
  },
  methods: {
    initCommonOptions() {
      this.playerCommonOptions = {
        videoBuffer: 0.5, // 最大缓冲时长，单位秒
        isResize: true,
        decoder: "/jessibuca/ff.js", //解码器
        loadingText: "加载中",
        hasAudio: typeof (this.hasAudio) == "undefined" ? true : this.hasAudio,
        debug: false,
        supportDblclickFullscreen: false, // 是否支持屏幕的双击事件，触发全屏，取消全屏事件。
        operateBtns: {
          fullscreen: false,
          screenshot: false,
          play: false,
          audio: false,
        },
        record: "record",
        vod: this.vod,
        forceNoOffscreen: this.forceNoOffscreen,
        isNotMute: this.isNotMute
      }
    },
    initPlayerEvent() {
      this.jessibuca.on("error", (error) => {
        console.log('发生错误，销毁播放器，并重建播放器，错误信息：' + error)
        this.$message.error('播放错误')
        this.jessibuca.destroy()
        let container = this.$refs.container
        let newPlayer = new Jessibuca(Object.assign({container: container}, this.playerCommonOptions))
        this.jessibuca = newPlayer
        this.$emit('setvideoPlayer', newPlayer)
      });
      this.jessibuca.on("timeout", () => {
        console.log('播放超时,销毁播放器，并重建播放器')
        this.$message.error('播放超时')
        this.jessibuca.destroy()
        let container = this.$refs.container
        let newPlayer = new Jessibuca(Object.assign({container: container}, this.playerCommonOptions))
        this.jessibuca = newPlayer
        this.$emit('setvideoPlayer', newPlayer)
      });
    },
    selectContainer() {
      let container = this.$refs.container
      container.style.border = '1px solid red'
      if (this.fullScreen) return
      let player = this.jessibuca
      if (player.playing) { //正在播放，弹出提示
        const self = this
        this.$confirm({
          title: '是否重新选择视频观看?',
          content: '点击确定将关闭本视频，可以选择其他视频观看',
          onOk() {
            player.destroy()
            let newPlayer = new Jessibuca(Object.assign({container: container}, self.playerCommonOptions))
            self.jessibuca = newPlayer
            self.$emit('setvideoPlayer', newPlayer)
          },
          onCancel() {
          },
        });
      } else {
        this.$message.success('选择预览窗口成功，请打开视频列表选择一个视频')
        this.$emit('setvideoPlayer', player)
      }
    },
    mouseLeave() {
      let container = this.$refs.container
      container.style.border = ''
    }
  }
}
</script>

<style scoped>

</style>