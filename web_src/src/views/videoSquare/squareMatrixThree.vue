<template>
  <div ref="videoRoot" style="margin-top: 10px">
    <a-row type="flex" :gutter="[1,2]">
      <a-col :flex="1">
        <div ref="container0" style="background-color: #000" @click="selectContainer(0)" @mouseleave="mouseLeave(0)"></div>
      </a-col>
      <a-col :flex="1">
        <div ref="container1" style="background-color: #000" @click="selectContainer(1)" @mouseleave="mouseLeave(1)"></div>
      </a-col>
      <a-col :flex="1">
        <div ref="container2" style="background-color: #000" @click="selectContainer(2)" @mouseleave="mouseLeave(2)"></div>
      </a-col>
    </a-row>
    <a-row type="flex" :gutter="[1, 2]">
      <a-col :flex="1">
        <div ref="container3" style="background-color: #000" @click="selectContainer(3)" @mouseleave="mouseLeave(3)"></div>
      </a-col>
      <a-col :flex="1">
        <div ref="container4" style="background-color: #000" @click="selectContainer(4)" @mouseleave="mouseLeave(4)"></div>
      </a-col>
      <a-col :flex="1">
        <div ref="container5" style="background-color: #000" @click="selectContainer(5)" @mouseleave="mouseLeave(5)"></div>
      </a-col>
    </a-row>
    <a-row type="flex" :gutter="[1, 2]">
      <a-col :flex="1">
        <div ref="container6" style="background-color: #000" @click="selectContainer(6)" @mouseleave="mouseLeave(6)"></div>
      </a-col>
      <a-col :flex="1">
        <div ref="container7" style="background-color: #000" @click="selectContainer(7)" @mouseleave="mouseLeave(7)"></div>
      </a-col>
      <a-col :flex="1">
        <div ref="container8" style="background-color: #000" @click="selectContainer(8)" @mouseleave="mouseLeave(8)"></div>
      </a-col>
    </a-row>
  </div>
</template>

<script>
import Jessibuca from '@/core/jessibuca/renderer'

export default {
  name: "squareMatrixThree",
  props: ['fullScreen'],
  data() {
    return {
      playerCommonOptions: {},
      players: {},
      playerContainerArr: []
    }
  },
  created() {
    this.initCommonOptions()
  },
  mounted() {
    for (let i = 0; i < 9; i++) {
      let key = 'container' + i
      let container = this.$refs[key]
      let options = Object.assign({container: container}, this.playerCommonOptions);
      container.style.height = (9 / 16) * container.clientWidth + "px"
      let jessibuca = new Jessibuca(options)
      this.players[key] = jessibuca
      this.initPlayerEvent(jessibuca, i)
      this.playerContainerArr.push(container);
    }
    //传递全屏元素
    this.$emit('setFullScreenEle', this.$refs.videoRoot, this.playerContainerArr, this.players)
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
    mouseLeave(num) {
      let container = this.$refs['container' + num]
      container.style.border = ''
    },
    selectContainer(num) {
      let container = this.$refs['container' + num]
      container.style.border = '1px solid red'
      if (this.fullScreen) return
      let player = this.players['container' + num]
      if (player.playing) { //正在播放，弹出提示
        const self = this
        this.$confirm({
          title: '是否重新选择视频观看?',
          content: '点击确定将关闭本视频，可以选择其他视频观看',
          onOk() {
            player.destroy()
            let newPlayer = new Jessibuca(Object.assign({container: container}, self.playerCommonOptions))
            self.players['container' + num] = newPlayer
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
    initPlayerEvent(player, num) {
      player.on("error", (error) => {
        console.log('发生错误，销毁播放器，并重建播放器，错误信息：' + error)
        this.$message.error('播放错误')
        player.destroy()
        let key = 'container' + num
        let container = this.$refs[key]
        let newPlayer = new Jessibuca(Object.assign({container: container}, this.playerCommonOptions))
        this.players[key] = newPlayer
        this.$emit('setvideoPlayer', newPlayer)
      });
      player.on("timeout", () => {
        console.log('播放超时,销毁播放器，并重建播放器')
        this.$message.error('播放超时')
        player.destroy()
        let key = 'container' + num
        let container = this.$refs[key]
        let newPlayer = new Jessibuca(Object.assign({container: container}, this.playerCommonOptions))
        this.players[key] = newPlayer
        this.$emit('setvideoPlayer', newPlayer)
      });
    }
  }
}
</script>

<style scoped>
:fullscreen { /*设置全屏样式*/
  background: #ffffff;
  overflow: hidden;
}
</style>