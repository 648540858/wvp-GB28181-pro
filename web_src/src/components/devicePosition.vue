<template>
    <div id="devicePosition" style="height: 100%">
        <el-container style="height: 100%">
            <el-header>
                <uiHeader></uiHeader>
            </el-header>
            <el-main>
                <div style="background-color: #ffffff; position: relative; padding: 1rem 0.5rem 0.5rem 0.5rem; text-align: center;">
                    <span style="font-size: 1rem; font-weight: 500">设备定位 ({{ parentChannelId == 0 ? deviceId : parentChannelId }})</span>
                </div>
                <div style="background-color: #ffffff; margin-bottom: 1rem; position: relative; padding: 0.5rem; text-align: left; font-size: 14px;">
                    <el-button icon="el-icon-arrow-left" size="mini" style="margin-right: 1rem" type="primary" @click="showDevice">返回</el-button>
                    <!-- <span class="demonstration">从</span> -->
                    <el-date-picker v-model="searchFrom" type="datetime" placeholder="选择开始日期时间" default-time="00:00:00" size="mini" style="width: 11rem;" align="right" :picker-options="pickerOptions"></el-date-picker>
                    <el-date-picker v-model="searchTo" type="datetime" placeholder="选择结束日期时间" default-time="00:00:00" size="mini" style="width: 11rem;" align="right" :picker-options="pickerOptions"></el-date-picker>
                    <el-button-group>
                        <el-button icon="el-icon-search" size="mini" type="primary" @click="showHistoryPath">历史轨迹</el-button>
                        <el-button icon="el-icon-search" size="mini" style="margin-right: 1rem" type="primary" @click="showLatestPosition">最新位置</el-button>
                    </el-button-group>
                    <el-tag style="width: 5rem; text-align: center" size="medium">过期时间</el-tag>
                    <el-input-number size="mini" v-model="expired" :min="300" :controls="false" style="width: 4rem;"></el-input-number>
                    <el-tag style="width: 5rem; text-align: center" size="medium">上报周期</el-tag>
                    <el-input-number size="mini" v-model="interval" :min="1" :controls="false" style="width: 4rem;"></el-input-number>
                    <el-button-group>
                        <el-button icon="el-icon-search" size="mini" type="primary" @click="subscribeMobilePosition">位置订阅</el-button>
                        <el-button icon="el-icon-search" size="mini" type="primary" @click="unSubscribeMobilePosition">取消订阅</el-button>
                    </el-button-group>
                    <el-checkbox size="mini" style="margin-right: 1rem; float: right" v-model="autoList" @change="autoListChange" >自动刷新</el-checkbox>
                </div>
                <div class="mapContainer" style="background-color: #ffffff; position: relative; padding: 1rem 0.5rem 0.5rem 0.5rem; text-align: center; height: calc(100% - 10rem);">
                    <div class="baidumap" id="allmap"></div>
                </div>
            </el-main>
        </el-container>
    </div>
</template>

<script>
import uiHeader from "./UiHeader.vue";
import moment from "moment";
import geoTools from "./GeoConvertTools.js";
export default {
    name: "devicePosition",
    components: {
        uiHeader,
    },
    data() {
        return {
            pickerOptions: {
                shortcuts: [{
                    text: '今天',
                    onClick(picker) {
                        picker.$emit('pick', new Date());
                    }
                }, {
                    text: '昨天',
                    onClick(picker) {
                        const date = new Date();
                        date.setTime(date.getTime() - 3600 * 1000 * 24);
                        picker.$emit('pick', date);
                    }
                }, {
                    text: '一周前',
                    onClick(picker) {
                        const date = new Date();
                        date.setTime(date.getTime() - 3600 * 1000 * 24 * 7);
                        picker.$emit('pick', date);
                    }
                }]
            },
            deviceId: this.$route.params.deviceId,
            showHistoryPosition: false, //显示历史轨迹
            startTime: null,
            endTime: null,
            searchFrom: null,
            searchTo: null,
            expired: 600,
            interval: 5,
            mobilePositionList: [],
            mapPointList: [],
            parentChannelId: this.$route.params.parentChannelId,
            updateLooper: 0, //数据刷新轮训标志
            total: 0,
            beforeUrl: "/videoList",
            isLoging: false,
            autoList: false,
        };
    },
    mounted() {
        this.initData();
        this.initBaiduMap();
        if (this.autoList) {
            this.updateLooper = setInterval(this.initData, 5000);
        }
    },
    destroyed() {
        // this.$destroy("videojs");
        clearTimeout(this.updateLooper);
    },
    methods: {
        initData: function () {
            // if (this.parentChannelId == "" || this.parentChannelId == 0) {
            //     this.getDeviceChannelList();
            // } else {
            //     this.showSubchannels();
            // }
        },
        initParam: function () {
            // this.deviceId = this.$route.params.deviceId;
            // this.parentChannelId = this.$route.params.parentChannelId;
            // this.currentPage = parseInt(this.$route.params.page);
            // this.count = parseInt(this.$route.params.count);
            // if (this.parentChannelId == "" || this.parentChannelId == 0) {
            //     this.beforeUrl = "/videoList";
            // }
        },
        initBaiduMap() {
            this.map = new BMap.Map("allmap"); // 创建地图实例
            let points = [];
            let point = new BMap.Point(116.231398, 39.567445); // 创建点坐标
            this.map.centerAndZoom(point, 5); // 初始化地图，设置中心点坐标和地图级别
            this.map.enableScrollWheelZoom(true); //开启鼠标滚轮缩放
            this.map.addControl(new BMap.NavigationControl());
            this.map.addControl(new BMap.ScaleControl());
            this.map.addControl(new BMap.OverviewMapControl());
            this.map.addControl(new BMap.MapTypeControl());
            //map.setMapStyle({ style: 'midnight' }) //地图风格
        },
        currentChange: function (val) {
            // var url = `/${this.$router.currentRoute.name}/${this.deviceId}/${this.parentChannelId}/${this.count}/${val}`;
            // console.log(url);
            // this.$router.push(url).then(() => {
            //     this.initParam();
            //     this.initData();
            // });
        },
        handleSizeChange: function (val) {
            // var url = `/${this.$router.currentRoute.name}/${this.$router.params.deviceId}/${this.$router.params.parentChannelId}/${val}/1`;
            // this.$router.push(url).then(() => {
            //     this.initParam();
            //     this.initData();
            // });
        },
        showDevice: function () {
            this.$router.push(this.beforeUrl).then(() => {
                this.initParam();
                this.initData();
            });
        },
        autoListChange: function () {
            if (this.autoList) {
                this.updateLooper = setInterval(this.initData, 1500);
            } else {
                window.clearInterval(this.updateLooper);
            }
        },
        showHistoryPath: function () {
            this.map.clearOverlays();
            this.mapPointList = [];
            this.mobilePositionList = [];
            if (!!this.searchFrom) {
                this.startTime = this.toGBString(this.searchFrom);
                console.log(this.startTime);
            } else{
                this.startTime = null;
            }
            if (!!this.searchTo) {
                this.endTime = this.toGBString(this.searchTo);
                console.log(this.endTime);
            } else {
                this.endTime = null;
            }
            let self = this;
            this.$axios.get(`/api/positions/${this.deviceId}/history`, {
                params: {
                    start: self.startTime,
                    end: self.endTime,
                },
            })
            .then(function (res) {
                self.total = res.data.length;
                self.mobilePositionList = res.data;
                console.log(self.mobilePositionList);
                // 防止出现表格错位
                self.$nextTick(() => {
                    self.showMarkPoints(self);
                });
            })
            .catch(function (error) {
                console.log(error);
            });
        },
        showLatestPosition: function() {
            this.map.clearOverlays();
            this.mapPointList = [];
            this.mobilePositionList = [];
            let self = this;
            this.$axios.get(`/api/positions/${this.deviceId}/latest`)
            .then(function (res) {
                console.log(res.data);
                self.total = res.data.length;
                self.mobilePositionList.push(res.data);
                console.log(self.mobilePositionList);
                // 防止出现表格错位
                self.$nextTick(() => {
                    self.showMarkPoints(self);
                });
            })
            .catch(function (error) {
                console.log(error);
            });
        },
        subscribeMobilePosition: function() {
            let self = this;
            this.$axios.get(`/api/positions/${this.deviceId}/subscribe`, {
                params: {
                    expires: self.expired,
                    interval: self.interval,
                },
            })
            .then(function (res) {
                console.log(res.data);
            })
            .catch(function (error) {
                console.log(error);
            });
        },
        unSubscribeMobilePosition: function() {
            let self = this;
            this.$axios.get(`/api/positions/${this.deviceId}/subscribe`, {
                params: {
                    expires: 0,
                    interval: self.interval,
                },
            })
            .then(function (res) {
                console.log(res.data);
            })
            .catch(function (error) {
                console.log(error);
            });
        },
        toGBString: function (dateTime) {
            return (
                dateTime.getFullYear() + 
                "-" + this.twoDigits(dateTime.getMonth() + 1) +
                "-" + this.twoDigits(dateTime.getDate()) +
                "T" + this.twoDigits(dateTime.getHours()) +
                ":" + this.twoDigits(dateTime.getMinutes()) +
                ":" + this.twoDigits(dateTime.getSeconds())
            );
        },
        twoDigits: function (num) {
            if (num < 10) {
                return "0" + num;
            } else {
                return "" + num;
            }
        },
        showMarkPoints: function(self) {
            let that = self;
            let npointJ = null;
            let npointW = null;
            let point = null;
            for (let i = 0; i < self.mobilePositionList.length; i++) {
                if (self.mobilePositionList[i].geodeticSystem == "BD-09") {
                    npointJ = self.mobilePositionList[i].cnLng;
                    npointW = self.mobilePositionList[i].cnLat;
                    point = new BMap.Point(npointJ, npointW);
                } else {
                    npointJ = self.mobilePositionList[i].longitude;
                    npointW = self.mobilePositionList[i].latitude;
                    let bd2 = geoTools.GPSToBaidu(npointJ, npointW);
                    point = new BMap.Point(bd2.lat, bd2.lng);
                }
                self.mapPointList.push(point);
                let marker = new BMap.Marker(point); // 创建标注
                self.map.addOverlay(marker); // 将标注添加到地图中
                //提示信息  可以解析 HTML标签以及CSS
                let infoWindow = new BMap.InfoWindow(`<p style='text-align:left;font-weight:800'>设备: ${self.mobilePositionList[i].deviceId}</p>
                            <p style='text-align:left;font-weight:0'>时间: ${self.mobilePositionList[i].time}</p>`);
                // 鼠标移上标注点要发生的事
                marker.addEventListener("mouseover", function () {
                    this.openInfoWindow(infoWindow);
                });
                // 鼠标移开标注点要发生的事
                marker.addEventListener("mouseout", function () {
                    this.closeInfoWindow(infoWindow);
                });
                // 鼠标点击标注点要发生的事情
                marker.addEventListener("click", function () {
                    alert("点击");
                });
            }
            let view = that.map.getViewport(eval(self.mapPointList));
            that.map.centerAndZoom(view.center, view.zoom);
        },
    },
};
</script>

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
