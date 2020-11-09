<template>
<div id="app">
    <el-container>
        <el-header>
            <uiHeader></uiHeader>
        </el-header>
        <el-main>
            <div style="background-color: #FFFFFF; margin-bottom: 1rem; position: relative; padding: 0.5rem; text-align: left;">
                <span style="font-size: 1rem; font-weight: bold;">控制台</span>
                <div style="position: absolute; right: 1rem; top: 0.3rem;">
                    <el-popover placement="bottom" width="750" height="300" trigger="click">
                        <div style="height: 600px;overflow:auto;">
                            <table class="table-c" cellspacing="0">
                                <tr v-for="(value, key, index) in serverConfig">
                                    <td style="width: 18rem; text-align: right;">{{ key }}</td>
                                    <td style="width: 33rem; text-align:left">{{ value }}</td>
                                </tr>
                            </table>
                        </div>
                        <el-button type="primary" slot="reference" size="mini" @click="getServerConfig()">查看服务器配置</el-button>
                    </el-popover>
                    <el-button style="margin-left: 1rem;" type="danger" size="mini" @click="reStartServer()">重启服务器</el-button>
                </div>
            </div>
            <el-row :gutter="30">
                <el-col :span="12">
                    <div class="control-table" id="ThreadsLoad">table1</div>
                </el-col>
                <el-col :span="12">
                    <div class="control-table" id="WorkThreadsLoad">table2</div>
                </el-col>
            </el-row>
            <el-table :data="allSessionData" style="margin-top: 1rem;">
                <el-table-column prop="peer_ip" label="远端"></el-table-column>
                <el-table-column prop="local_ip" label="本地"></el-table-column>
                <el-table-column prop="typeid" label="类型"></el-table-column>
                <el-table-column align="right">
                    <template slot="header" slot-scope="scope">
                        <el-button icon="el-icon-refresh-right" circle @click="getAllSession()"></el-button>
                    </template>
                    <template slot-scope="scope">
                        <el-button @click.native.prevent="deleteRow(scope.$index, allSessionData)" type="text" size="small">移除</el-button>
                    </template>
                </el-table-column>
            </el-table>

        </el-main>
        <!-- <el-footer style="position: absolute; bottom: 0; width: 100%;">ZLMediaKit-VUE_UI v1</el-footer> -->
    </el-container>

</div>
</template>

<script>
import uiHeader from './UiHeader.vue'

import echarts from 'echarts';
export default {
    name: 'app',
    components: {
        echarts,
        uiHeader
    },
    data() {
        return {
            tableOption: {
                // legend: {},
                xAxis: {},
                yAxis: {},
                label: {},
                tooltip: {},
                dataZoom: [],
                series: []
            },
            table1Option: {
                // legend: {},
                xAxis: {},
                yAxis: {},
                label: {},
                tooltip: {},
                series: []
            },
            mChart: null,
            mChart1: null,
            charZoomStart: 0,
            charZoomEnd: 100,
            chartInterval: 0, //更新图表统计图定时任务标识
            allSessionData: [],
            visible: false,
            serverConfig: {}
        };
    },
    mounted() {
        this.getAllSession();
        this.initTable();
        this.updateData();
        this.chartInterval = setInterval(this.updateData, 3000);
    },
    destroyed() {
        clearInterval(this.chartInterval); //释放定时任务
    },
    methods: {
        updateData: function () {
            this.getThreadsLoad();
        },
        /**
         * 获取线程状态
         */
        getThreadsLoad: function () {
            let that = this;
            this.$axios({
                method: 'get',
                url: '/zlm/index/api/getThreadsLoad'
            }).then(function (res) {
                if (res.data.code == 0) {
                    that.tableOption.xAxis.data.push(new Date().toLocaleTimeString('chinese', {
                        hour12: false
                    }));
                    that.table1Option.xAxis.data.push(new Date().toLocaleTimeString('chinese', {
                        hour12: false
                    }));

                    for (var i = 0; i < res.data.data.length; i++) {
                        if (that.tableOption.series[i] === undefined) {
                            let data = {
                                data: [],
                                type: 'line'
                            };
                            let data1 = {
                                data: [],
                                type: 'line'
                            };
                            data.data.push(res.data.data[i].delay);
                            data1.data.push(res.data.data[i].load);
                            that.tableOption.series.push(data);
                            that.table1Option.series.push(data1);
                        } else {
                            that.tableOption.series[i].data.push(res.data.data[i].delay);
                            that.table1Option.series[i].data.push(res.data.data[i].load);
                        }
                    }
                    that.tableOption.dataZoom[0].start = that.charZoomStart;
                    that.tableOption.dataZoom[0].end = that.charZoomEnd;
                    that.table1Option.dataZoom[0].start = that.charZoomStart;
                    that.table1Option.dataZoom[0].end = that.charZoomEnd;
                    //that.myChart = echarts.init(document.getElementById('ThreadsLoad'));
                    that.myChart.setOption(that.tableOption, true);
                    // that.myChart1 = echarts.init(document.getElementById('WorkThreadsLoad'));
                    that.myChart1.setOption(that.table1Option, true);
                }
            });
        },
        initTable: function () {
            let that = this;
            this.tableOption.xAxis = {
                type: 'category',
                data: [], // x轴数据
                name: '时间', // x轴名称
                // x轴名称样式
                nameTextStyle: {
                    fontWeight: 300,
                    fontSize: 15
                }
            };
            this.tableOption.yAxis = {
                type: 'value',
                name: '延迟率', // y轴名称
                boundaryGap: [0, '100%'],
                max: 100,
                axisLabel: {
                    show: true,
                    interval: 'auto',
                    formatter: '{value} %'
                },
                // y轴名称样式
                nameTextStyle: {
                    fontWeight: 300,
                    fontSize: 15
                }
            };
            this.tableOption.dataZoom = [{
                show: true,
                start: this.charZoomStart,
                end: this.charZoomEnd
            }];
            this.myChart = echarts.init(document.getElementById('ThreadsLoad'));
            this.myChart.setOption(this.tableOption);
            this.myChart.on('dataZoom', function (event) {
                if (event.batch) {
                    that.charZoomStart = event.batch[0].start;
                    that.charZoomEnd = event.batch[0].end;
                } else {
                    that.charZoomStart = event.start;
                    that.charZoomEnd = event.end;
                }
            });

            this.table1Option.xAxis = {
                type: 'category',
                data: [], // x轴数据
                name: '时间', // x轴名称
                // x轴名称样式
                nameTextStyle: {
                    fontWeight: 300,
                    fontSize: 15
                }
            };
            this.table1Option.yAxis = {
                type: 'value',
                name: '负载率', // y轴名称
                boundaryGap: [0, '100%'],
                max: 100,
                axisLabel: {
                    show: true,
                    interval: 'auto',
                    formatter: '{value} %'
                },
                // y轴名称样式
                nameTextStyle: {
                    fontWeight: 300,
                    fontSize: 15
                }
            };
            this.table1Option.dataZoom = [{
                show: true,
                start: this.charZoomStart,
                end: this.charZoomEnd
            }];
            this.myChart1 = echarts.init(document.getElementById('WorkThreadsLoad'));
            this.myChart1.setOption(this.table1Option);
            this.myChart1.on('dataZoom', function (event) {
                if (event.batch) {
                    that.charZoomStart = event.batch[0].start;
                    that.charZoomEnd = event.batch[0].end;
                } else {
                    that.charZoomStart = event.start;
                    that.charZoomEnd = event.end;
                }
            });
        },

        getAllSession: function () {
            let that = this;
            that.allSessionData = [];
            console.log("地址：" + '/zlm/index/api/getAllSession');
            this.$axios({
                method: 'get',
                url: '/zlm/index/api/getAllSession'
            }).then(function (res) {
                res.data.data.forEach(item => {
                    let data = {
                        peer_ip: item.peer_ip,
                        local_ip: item.local_ip,
                        typeid: item.typeid,
                        id: item.id
                    };
                    that.allSessionData.push(data);
                });
            });
        },
        getServerConfig: function () {
            let that = this;
            this.$axios({
                method: 'get',
                url: '/zlm/index/api/getServerConfig'
            }).then(function (res) {
                that.serverConfig = res.data.data[0];
                that.visible = true;
            });
        },
        reStartServer: function () {
            let that = this;
            this.$confirm('此操作将重启媒体服务器, 是否继续?', '提示', {
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                type: 'warning'
            }).then(() => {
                let that = this;
                this.$axios({
                    method: 'get',
                    url: '/zlm/index/api/restartServer'
                }).then(function (res) {
                    that.getAllSession();
                    if (res.data.code == 0) {
                        that.$message({
                            type: 'success',
                            message: '操作完成'
                        });
                    }
                });
            });
        },
        deleteRow: function (index, tabledata) {
            let that = this;
            this.$confirm('此操作将断开该通信链路, 是否继续?', '提示', {
                    confirmButtonText: '确定',
                    cancelButtonText: '取消',
                    type: 'warning'
                })
                .then(() => {
                    that.deleteSession(tabledata[index].id);
                })
                .catch(() => {
                    console.log('id：' + JSON.stringify(tabledata[index]));
                    this.$message({
                        type: 'info',
                        message: '已取消删除'
                    });
                });
            console.log(JSON.stringify(tabledata[index]));
        },
        deleteSession: function (id) {
            let that = this;
            this.$axios({
                method: 'get',
                url: '/zlm/index/api/kick_session&id=' + id
            }).then(function (res) {
                that.getAllSession();
                that.$message({
                    type: 'success',
                    message: '删除成功!'
                });
            });
        }
    }
};
</script>

<style>
#app {
    height: 100%;
}

.control-table {
    background-color: #ffffff;
    height: 25rem;
}

.table-c {
    border-right: 1px solid #dcdcdc;
    border-bottom: 1px solid #dcdcdc;
}

.table-c td {
    border-left: 1px solid #dcdcdc;
    border-top: 1px solid #dcdcdc;
    padding: 0.2rem;
}

.el-table {
    width: 99.9% !important;
}
</style>
