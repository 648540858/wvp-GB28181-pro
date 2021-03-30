<template>
    <div id="player">
        <div id="easyplayer"></div>   
    </div>
</template>

<script>
export default {
    name: 'player',
    data() {
        return {
            easyPlayer: null
        };
    },
    props: ['videoUrl', 'error', 'hasaudio'],
    mounted () {
       this.$nextTick(() =>{
           console.log("初始化时的地址为: " + this.videoUrl)
            this.easyPlayer = new WasmPlayer(null, 'easyplayer', this.eventcallbacK)
            this.easyPlayer.play(this.videoUrl, 1)
        })
    },
    watch:{
        videoUrl(newData, oldData){
            this.easyPlayer.destroy()
            this.easyPlayer = new WasmPlayer(null, 'easyplayer', this.eventcallbacK)
            this.easyPlayer.play(newData, 1)
        },
        immediate:true
    },
    methods: {
        play: function (url) {
            this.easyPlayer = new WasmPlayer(null, 'easyplayer', this.eventcallbacK)
            this.easyPlayer.play(url, 1)
        },
        pause: function () {
            this.easyPlayer.destroy();
        },
        eventcallbacK: function(type, message) {
            console.log("player 事件回调")
            console.log(type)
            console.log(message)
        }
    },
}
</script>

<style>
    .LodingTitle {
        min-width: 70px;
    }
    /* 隐藏logo */
    /* .iconqingxiLOGO {
        display: none !important;
    } */
    
</style>