<template>
  <div id="devicePosition" style="height: 100%">
    <el-container style="height: 100%">
      <el-header>
        <uiHeader></uiHeader>
      </el-header>
      <el-container v-loading="loading" element-loading-text="拼命加载中">
        <el-aside width="300px" style="background-color: #ffffff">
          <div style="text-align: center;padding-top: 20px;">设备列表</div>
          <el-menu  v-loading="loading">
            <el-submenu v-for="device in deviceList" :key="device.deviceId" :index="device.deviceId" @click="sendDevicePush(item)">
              <template slot="title" >
                <i class="el-icon-location-outline"></i>
                {{device.name}}
              </template>
              <ChannelTree :device="device" @sendDevicePush="sendDevicePush"></ChannelTree>
            </el-submenu>
          </el-menu>
        </el-aside>
          <el-container>
            <!-- <LivePlay></LivePlay> -->
            <el-header height="40px" style="text-align: left;font-size: 17px;line-height: 40px;">
              分屏:
              <i class="el-icon-full-screen btn" :class="{active:spilt==1}" @click="spilt=1"/>
              <i class="el-icon-menu btn" :class="{active:spilt==4}" @click="spilt=4"/>
              <i class="el-icon-s-grid btn" :class="{active:spilt==9}" @click="spilt=9"/>
            </el-header>
            <el-main>
              <div style="width: 100%;height: calc( 100vh - 110px );display: flex;flex-wrap: wrap;background-color: #000;">
                <div v-for="i in spilt" :key="i" class="play-box"
                    :style="liveStyle" :class="{redborder:playerIdx == (i-1)}"
                    @click="playerIdx = (i-1)"
                >
                  <div v-if="!videoUrl[i-1]" style="color: #ffffff;font-size: 30px;font-weight: bold;">{{i}}</div>
                  <player v-else :ref="'player'+i" :videoUrl="videoUrl[i-1]"  fluent autoplay :height="true"
                          :idx="'player'+i" @screenshot="shot" @destroy="destroy"></player>
                  <!-- <player v-else ref="'player'+i" :idx="'player'+i" :visible.sync="showVideoDialog" :videoUrl="videoUrl[i-1]"  :height="true" :hasAudio="hasAudio" fluent autoplay live ></player> -->
                </div>
              </div>
            </el-main>
          </el-container>
      </el-container>
    </el-container>
  </div>
</template>

<script>
  import uiHeader from "./UiHeader.vue";
  import player from './jessibuca.vue'
  import ChannelTree from './channelTree.vue'

  export default {
    name: "live",
    components: {
      uiHeader, player, ChannelTree
    },
    data() {
      return {
        showVideoDialog: true,
        hasAudio: false,
        videoUrl:[''],
        spilt:1,//分屏
        playerIdx:0,//激活播放器

        deviceList: [], //设备列表
        currentDevice: {}, //当前操作设备对象

        videoComponentList: [],
        updateLooper: 0, //数据刷新轮训标志
        currentDeviceChannelsLenth:0,
        winHeight: window.innerHeight - 200,
        currentPage:1,
        count:15,
        total:0,
        getDeviceListLoading: false,

        //channel
        searchSrt: "",
        channelType: "",
        online: "",
        channelTotal:0,
        deviceChannelList:[],
        loading:false
      };
    },
    mounted() {
      this.initData();

    },
    created(){
      this.checkPlayByParam()
    },

    computed:{
      liveStyle(){
        if(this.spilt==1){
          return {width:'100%',height:'100%'}
        }else if(this.spilt==4){
          return {width:'49%',height:'49%'}
        }else if(this.spilt==9){
          return {width:'32%',height:'32%'}
        }
      }
    },
    watch:{
      spilt(newValue){
        console.log("切换画幅;"+newValue)
        let that = this
        for (let i = 1; i <= newValue; i++) {
          if(!that.$refs['player'+i]){
            continue
          }
          this.$nextTick(()=>{
            if(that.$refs['player'+i] instanceof Array){
              that.$refs['player'+i][0].resize()
            }else {
              that.$refs['player'+i].resize()
            }
          })

        }
        window.localStorage.setItem('split',newValue)
      },
      '$route.fullPath':'checkPlayByParam'
    },
    destroyed() {
      clearTimeout(this.updateLooper);
    },
    methods: {
      initData: function () {
        this.getDeviceList();

      },
      destroy(idx) {
        console.log(idx);
        this.clear(idx.substring(idx.length-1))
      },
      getDeviceList: function() {
        let that = this;
        this.$axios({
          method: 'get',
          url:`/api/device/query/devices`,
          params: {
            page: that.currentPage,
            count: that.count
          }
        }).then(function (res) {
          console.log(res.data.list);
          that.total = res.data.total;

          that.deviceList = res.data.list.map(item=>{return {deviceChannelList:[],...item}});
          that.getDeviceListLoading = false;
        }).catch(function (error) {
          console.log(error);
          that.getDeviceListLoading = false;
        });
      },
      //通知设备上传媒体流
      sendDevicePush: function (itemData) {
        if(itemData.status===0){
          this.$message.error('设备离线!');
          return
        }
        this.save(itemData)
        let deviceId = itemData.deviceId;
        // this.isLoging = true;
        let channelId = itemData.channelId;
        console.log("通知设备推流1：" + deviceId + " : " + channelId );
        let idxTmp = this.playerIdx
        let that = this;
        this.loading = true
        this.$axios({
          method: 'get',
          url: '/api/play/start/' + deviceId + '/' + channelId
        }).then(function (res) {
          // that.isLoging = false;
          console.log('=====----=====')
          console.log(res)
          if (res.data.code == 0 && res.data.data) {
            itemData.playUrl = res.data.data.httpsFlv
            that.setPlayUrl(res.data.data.ws_flv,idxTmp)
          }else {
            that.$message.error(res.data.msg);
          }
        }).catch(function (e) {
        }).finally(()=>{
          that.loading = false
        });
      },
      setPlayUrl(url,idx){
        this.$set(this.videoUrl,idx,url)
        let _this = this
        setTimeout(()=>{
          window.localStorage.setItem('videoUrl',JSON.stringify(_this.videoUrl))
        },100)

      },
      checkPlayByParam(){
        let {deviceId,channelId} = this.$route.query
        if(deviceId && channelId){
          this.sendDevicePush({deviceId,channelId})
        }
      },
      convertImageToCanvas(image) {
        var canvas = document.createElement("canvas");
        canvas.width = image.width;
        canvas.height = image.height;
        canvas.getContext("2d").drawImage(image, 0, 0);
        return canvas;
      },
      shot(e){
        // console.log(e)
        // send({code:'image',data:e})
        var base64ToBlob = function(code) {
          let parts = code.split(';base64,');
          let contentType = parts[0].split(':')[1];
          let raw = window.atob(parts[1]);
          let rawLength = raw.length;
          let uInt8Array = new Uint8Array(rawLength);
          for(let i = 0; i < rawLength; ++i) {
              uInt8Array[i] = raw.charCodeAt(i);
          }
          return new Blob([uInt8Array], {
              type: contentType
          });
        };
        let aLink = document.createElement('a');
        let blob = base64ToBlob(e); //new Blob([content]);
        let evt = document.createEvent("HTMLEvents");
        evt.initEvent("click", true, true); //initEvent 不加后两个参数在FF下会报错  事件类型，是否冒泡，是否阻止浏览器的默认行为
        aLink.download = '截图';
        aLink.href = URL.createObjectURL(blob);
        aLink.click();
      },
      save(item){
        let dataStr = window.localStorage.getItem('playData') || '[]'
        let data = JSON.parse(dataStr);
        data[this.playerIdx] = item
        window.localStorage.setItem('playData',JSON.stringify(data))
      },
      clear(idx) {
        let dataStr = window.localStorage.getItem('playData') || '[]'
        let data = JSON.parse(dataStr);
        data[idx-1] = null;
        console.log(data);
        window.localStorage.setItem('playData',JSON.stringify(data))
      },
      loadAndPlay(){
        let dataStr = window.localStorage.getItem('playData') || '[]'
        let data = JSON.parse(dataStr);

        data.forEach((item,i)=>{
          if(item){
            this.playerIdx = i
            this.sendDevicePush(item)
          }
        })
      }
    }
  };
</script>
<style>
  .btn{
    margin: 0 10px;

  }
  .btn:hover{
      color: #409EFF;
  }
  .btn.active{
    color: #409EFF;

  }
  .redborder{
    border: 2px solid red !important;
  }
  .play-box{
    background-color: #000000;
    border: 2px solid #505050;
    display: flex;
    align-items: center;
    justify-content: center;
  }
</style>
<style>
  .videoList {
    display: flex;
    flex-wrap: wrap;
    align-content: flex-start;
  }

  .video-item {
    position: relative;
    width: 15rem;
    height: 10rem;
    margin-right: 1rem;
    background-color: #000000;
  }

  .video-item-img {
    position: absolute;
    top: 0;
    bottom: 0;
    left: 0;
    right: 0;
    margin: auto;
    width: 100%;
    height: 100%;
  }

  .video-item-img:after {
    content: "";
    display: inline-block;
    position: absolute;
    z-index: 2;
    top: 0;
    bottom: 0;
    left: 0;
    right: 0;
    margin: auto;
    width: 3rem;
    height: 3rem;
    background-image: url("../assets/loading.png");
    background-size: cover;
    background-color: #000000;
  }

  .video-item-title {
    position: absolute;
    bottom: 0;
    color: #000000;
    background-color: #ffffff;
    line-height: 1.5rem;
    padding: 0.3rem;
    width: 14.4rem;
  }

  .baidumap {
    width: 100%;
    height: 100%;
    border: none;
    position: absolute;
    left: 0;
    top: 0;
    right: 0;
    bottom: 0;
    margin: auto;
  }

  /* 去除百度地图版权那行字 和 百度logo */
  .baidumap > .BMap_cpyCtrl {
    display: none !important;
  }
  .baidumap > .anchorBL {
    display: none !important;
  }
</style>
