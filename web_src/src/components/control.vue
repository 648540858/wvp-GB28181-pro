<template>
  <div id="app" style="width: 100%">
    <div class="page-header">
      <div class="page-title">控制台</div>
      <div class="page-header-btn">
        节点选择:
        <el-select size="mini" @change="chooseMediaChange" style="width: 18rem; margin-right: 8rem;"
                   v-model="mediaServerChoose" placeholder="请选择" default-first-option>
          <el-option
            v-for="item in mediaServerList"
            :key="item.id"
            :label="item.id + '( ' + item.streamIp + ' )'"
            :value="item.id">
          </el-option>
        </el-select>
        <span>{{ loadCount }}</span>
      </div>
      <div class="page-header-btn">
        <el-popover placement="bottom" width="900" height="300" trigger="click">
          <div style="height: 600px; overflow:auto; padding: 20px">
            <el-descriptions v-for="(value, key, index) in serverConfig" :key="key" border :column="1"
                             style="margin-bottom: 1rem">
              <template slot="title">
                {{ key }}
              </template>
              <el-descriptions-item v-for="(value1, key1, index1) in serverConfig[key]" :key="key1">
                <template slot="label">
                  {{ getMediaKeyNameFromKey(key1) }}
                </template>
                {{ value1 }}
              </el-descriptions-item>
            </el-descriptions>
          </div>
          <el-button type="primary" slot="reference" size="mini" @click="getServerConfig()">媒体服务器配置</el-button>
        </el-popover>
        <el-popover placement="bottom" width="900" height="300" trigger="click">
          <div style="height: 600px;overflow:auto; padding: 20px">
            <el-descriptions title="国标配置" border :column="1">
              <template slot="extra">
                <el-button style="float: right;" type="primary" size="mini" icon="el-icon-document-copy" title="点击拷贝"
                           v-clipboard="JSON.stringify(wvpServerConfig.sip)|| ''"
                           @success="$message({type:'success', message:'成功拷贝到粘贴板'})"></el-button>
              </template>
              <el-descriptions-item v-for="(value, key, index) in wvpServerConfig.sip" :key="key">
                <template slot="label">
                  {{ getNameFromKey(key) }}
                </template>
                {{ value }}
              </el-descriptions-item>
            </el-descriptions>

            <div style="margin-top: 1rem">
              <el-descriptions title="基础配置" border :column="1">
                <template slot="extra">
                  <el-button style="float: right;" type="primary" size="mini" icon="el-icon-document-copy" title="点击拷贝"
                             v-clipboard="JSON.stringify(wvpServerConfig.base)|| ''"
                             @success="$message({type:'success', message:'成功拷贝到粘贴板'})"></el-button>
                </template>
                <el-descriptions-item v-for="(value, key, index) in wvpServerConfig.base" :key="key">
                  <template slot="label">
                    {{ getNameFromKey(key) }}
                  </template>
                  <div v-if="key === 'interfaceAuthenticationExcludes'">
                    <el-dropdown>
                                      <span class="el-dropdown-link">
                                        查看<i class="el-icon-arrow-down el-icon--right"></i>
                                      </span>
                      <el-dropdown-menu slot="dropdown">
                        <el-dropdown-item
                          v-for="(value, key, index) in wvpServerConfig.base.interfaceAuthenticationExcludes"
                          :key="key">{{ value }}
                        </el-dropdown-item>
                      </el-dropdown-menu>
                    </el-dropdown>
                  </div>
                  <div v-if="key !== 'interfaceAuthenticationExcludes'">
                    <div v-if="value === true">
                      已启用
                    </div>
                    <div v-if="value === false">
                      未启用
                    </div>
                    <div v-if="value !== true && value !== false">
                      {{ value }}
                    </div>
                  </div>

                </el-descriptions-item>
              </el-descriptions>
            </div>
            <div style="margin-top: 1rem">
              <el-descriptions title="版本信息" border :column="1">
                <template slot="extra">
                  <el-button style="float: right;" type="primary" size="mini" icon="el-icon-document-copy" title="点击拷贝"
                             v-clipboard="JSON.stringify(wvpServerVersion) || ''"
                             @success="$message({type:'success', message:'成功拷贝到粘贴板'})"></el-button>
                </template>
                <el-descriptions-item v-for="(value, key, index) in wvpServerVersion" :key="key">
                  <template slot="label">
                    {{ getNameFromKey(key) }}
                  </template>
                  {{ value }}
                </el-descriptions-item>
              </el-descriptions>


            </div>
          </div>
          <el-button type="primary" slot="reference" size="mini" @click="getWVPServerConfig()">信令服务器配置</el-button>
        </el-popover>
        <el-button style="margin-left: 1rem;" type="danger" size="mini" @click="reStartServer()">重启媒体服务器</el-button>
      </div>
    </div>
    <!--    <div style="background-color: #FFFFFF; margin-bottom: 1rem; position: relative; padding: 0.5rem; text-align: left;">-->
    <!--      <span style="font-size: 1rem; font-weight: bold;">控制台</span>-->
    <!--      <div style="position: absolute; right: 17rem; top: 0.3rem;">-->
    <!--        节点选择:-->
    <!--        <el-select size="mini" @change="chooseMediaChange" style="width: 18rem; margin-right: 8rem;"-->
    <!--                   v-model="mediaServerChoose" placeholder="请选择" default-first-option>-->
    <!--          <el-option-->
    <!--            v-for="item in mediaServerList"-->
    <!--            :key="item.id"-->
    <!--            :label="item.id + '( ' + item.streamIp + ' )'"-->
    <!--            :value="item.id">-->
    <!--          </el-option>-->
    <!--        </el-select>-->
    <!--        <span>{{ loadCount }}</span>-->
    <!--      </div>-->
    <!--      <div style="position: absolute; right: 1rem; top: 0.3rem;">-->
    <!--        <el-popover placement="bottom" width="900" height="300" trigger="click">-->
    <!--          <div style="height: 600px; overflow:auto; padding: 20px">-->
    <!--            <el-descriptions v-for="(value, key, index) in serverConfig" :key="key" border :column="1"-->
    <!--                             style="margin-bottom: 1rem">-->
    <!--              <template slot="title">-->
    <!--                {{ key }}-->
    <!--              </template>-->
    <!--              <el-descriptions-item v-for="(value1, key1, index1) in serverConfig[key]" :key="key1">-->
    <!--                <template slot="label">-->
    <!--                  {{ getMediaKeyNameFromKey(key1) }}-->
    <!--                </template>-->
    <!--                {{ value1 }}-->
    <!--              </el-descriptions-item>-->
    <!--            </el-descriptions>-->
    <!--          </div>-->
    <!--          <el-button type="primary" slot="reference" size="mini" @click="getServerConfig()">媒体服务器配置</el-button>-->
    <!--        </el-popover>-->
    <!--        <el-popover placement="bottom" width="900" height="300" trigger="click">-->
    <!--          <div style="height: 600px;overflow:auto; padding: 20px">-->
    <!--            <el-descriptions title="国标配置" border :column="1">-->
    <!--              <template slot="extra">-->
    <!--                <el-button style="float: right;" type="primary" size="mini" icon="el-icon-document-copy" title="点击拷贝"-->
    <!--                           v-clipboard="JSON.stringify(wvpServerConfig.sip)|| ''"-->
    <!--                           @success="$message({type:'success', message:'成功拷贝到粘贴板'})"></el-button>-->
    <!--              </template>-->
    <!--              <el-descriptions-item v-for="(value, key, index) in wvpServerConfig.sip" :key="key">-->
    <!--                <template slot="label">-->
    <!--                  {{ getNameFromKey(key) }}-->
    <!--                </template>-->
    <!--                {{ value }}-->
    <!--              </el-descriptions-item>-->
    <!--            </el-descriptions>-->

    <!--            <div style="margin-top: 1rem">-->
    <!--              <el-descriptions title="基础配置" border :column="1">-->
    <!--                <template slot="extra">-->
    <!--                  <el-button style="float: right;" type="primary" size="mini" icon="el-icon-document-copy" title="点击拷贝"-->
    <!--                             v-clipboard="JSON.stringify(wvpServerConfig.base)|| ''"-->
    <!--                             @success="$message({type:'success', message:'成功拷贝到粘贴板'})"></el-button>-->
    <!--                </template>-->
    <!--                <el-descriptions-item v-for="(value, key, index) in wvpServerConfig.base" :key="key">-->
    <!--                  <template slot="label">-->
    <!--                    {{ getNameFromKey(key) }}-->
    <!--                  </template>-->
    <!--                  <div v-if="key === 'interfaceAuthenticationExcludes'">-->
    <!--                    <el-dropdown>-->
    <!--                                      <span class="el-dropdown-link">-->
    <!--                                        查看<i class="el-icon-arrow-down el-icon&#45;&#45;right"></i>-->
    <!--                                      </span>-->
    <!--                      <el-dropdown-menu slot="dropdown">-->
    <!--                        <el-dropdown-item-->
    <!--                          v-for="(value, key, index) in wvpServerConfig.base.interfaceAuthenticationExcludes"-->
    <!--                          :key="key">{{ value }}-->
    <!--                        </el-dropdown-item>-->
    <!--                      </el-dropdown-menu>-->
    <!--                    </el-dropdown>-->
    <!--                  </div>-->
    <!--                  <div v-if="key !== 'interfaceAuthenticationExcludes'">-->
    <!--                    <div v-if="value === true">-->
    <!--                      已启用-->
    <!--                    </div>-->
    <!--                    <div v-if="value === false">-->
    <!--                      未启用-->
    <!--                    </div>-->
    <!--                    <div v-if="value !== true && value !== false">-->
    <!--                      {{ value }}-->
    <!--                    </div>-->
    <!--                  </div>-->

    <!--                </el-descriptions-item>-->
    <!--              </el-descriptions>-->
    <!--            </div>-->
    <!--            <div style="margin-top: 1rem">-->
    <!--              <el-descriptions title="版本信息" border :column="1">-->
    <!--                <template slot="extra">-->
    <!--                  <el-button style="float: right;" type="primary" size="mini" icon="el-icon-document-copy" title="点击拷贝"-->
    <!--                             v-clipboard="JSON.stringify(wvpServerVersion) || ''"-->
    <!--                             @success="$message({type:'success', message:'成功拷贝到粘贴板'})"></el-button>-->
    <!--                </template>-->
    <!--                <el-descriptions-item v-for="(value, key, index) in wvpServerVersion" :key="key">-->
    <!--                  <template slot="label">-->
    <!--                    {{ getNameFromKey(key) }}-->
    <!--                  </template>-->
    <!--                  {{ value }}-->
    <!--                </el-descriptions-item>-->
    <!--              </el-descriptions>-->


    <!--            </div>-->
    <!--          </div>-->
    <!--          <el-button type="primary" slot="reference" size="mini" @click="getWVPServerConfig()">信令服务器配置</el-button>-->
    <!--        </el-popover>-->
    <!--        <el-button style="margin-left: 1rem;" type="danger" size="mini" @click="reStartServer()">重启媒体服务器</el-button>-->
    <!--      </div>-->
    <!--    </div>-->
    <el-row style="width: 100%">
      <el-col :span="12">
        <div class="control-table" id="ThreadsLoad" style="margin-right:10px;">table1</div>
      </el-col>
      <el-col :span="12">
        <div class="control-table" id="WorkThreadsLoad" style="margin-left:10px;">table2</div>
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
          <el-button @click.native.prevent="deleteRow(scope.$index, allSessionData)" type="text" size="small">移除
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script>
import uiHeader from '../layout/UiHeader.vue'
import MediaServer from './service/MediaServer'

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
      wvpVisible: false,
      serverConfig: {},
      wvpServerConfig: {},
      wvpServerVersion: {},
      mediaServer: new MediaServer(),
      mediaServerChoose: null,
      loadCount: 0,
      mediaServerList: []
    };
  },
  mounted() {
    this.initTable()
    this.chartInterval = setInterval(this.updateData, 3000);
    this.mediaServer.getOnlineMediaServerList((data) => {
      this.mediaServerList = data.data;
      if (this.mediaServerList && this.mediaServerList.length > 0) {
        this.mediaServerChoose = this.mediaServerList[0].id
        this.loadCount = this.mediaServerList[0].count;
        this.updateData();
      }
    })
  },
  destroyed() {
    clearInterval(this.chartInterval); //释放定时任务
  },
  methods: {
    chooseMediaChange: function (val) {
      this.loadCount = 0
      this.initTable()
      this.updateData();
    },
    updateData: function () {
      this.getThreadsLoad();
      this.getLoadCount();
      this.getAllSession();
    },
    /**
     * 获取线程状态
     */
    getThreadsLoad: function () {
      let that = this;
      if (that.mediaServerChoose != null) {
        this.$axios({
          method: 'get',
          url: '/zlm/' + that.mediaServerChoose + '/index/api/getThreadsLoad'
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
            that.$nextTick(() => {
              that.myChart.resize()
              that.myChart1.resize()
            })
          }
        });
      }

    },
    getLoadCount: function () {
      let that = this;
      if (that.mediaServerChoose != null) {
        that.mediaServer.getMediaServer(that.mediaServerChoose, (data) => {
          if (data.code == 0) {
            that.loadCount = data.data.count
          }
        })
      }
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
      this.$axios({
        method: 'get',
        url: '/zlm/' + that.mediaServerChoose + '/index/api/getAllSession'
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
        url: '/zlm/' + that.mediaServerChoose + '/index/api/getServerConfig'
      }).then(function (res) {
        let info = res.data.data[0];
        let serverInfo = {}
        for (let i = 0; i < Object.keys(info).length; i++) {
          let key = Object.keys(info)[i];
          let group = key.substring(0, key.indexOf("."))
          let itemKey = key.substring(key.indexOf(".") + 1)
          if (!serverInfo[group]) serverInfo[group] = {}
          serverInfo[group][itemKey] = info[key]
        }

        that.serverConfig = serverInfo;
        that.visible = true;
      });
    },
    getWVPServerConfig: function () {
      let that = this;
      this.$axios({
        method: 'get',
        url: '/api/server/config'
      }).then(function (res) {
        console.log(res)
        that.wvpServerConfig = res.data.data;
        that.wvpVisible = true;
      });
      this.$axios({
        method: 'get',
        url: '/api/server/version'
      }).then(function (res) {
        console.log(res)
        that.wvpServerVersion = res.data.data;
        that.wvpVisible = true;
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
          url: '/zlm/' + that.mediaServerChoose + '/index/api/restartServer'
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
        url: '/zlm/' + that.mediaServerChoose + '/index/api/kick_session&id=' + id
      }).then(function (res) {
        that.getAllSession();
        that.$message({
          type: 'success',
          message: '删除成功!'
        });
      });
    },
    getNameFromKey: function (key) {
      let nameData = {
        "waitTrack": "等待编码信息",
        "interfaceAuthenticationExcludes": "不进行鉴权的接口",
        "playTimeout": "点播超时时间",
        "autoApplyPlay": "自动点播",
        "recordPushLive": "推流录像",
        "redisConfig": "自动配置redis",
        "thirdPartyGBIdReg": "stream信息正则",
        "savePositionHistory": "保存轨迹信息",
        "interfaceAuthentication": "接口鉴权",
        "serverId": "服务ID",
        "logInDatebase": "日志存储进数据库",
        "seniorSdp": "扩展SDP",
        "password": "密码",
        "port": "端口号",
        "keepaliveTimeOut": "心跳超时",
        "domain": "国标域",
        "ip": "IP地址",
        "monitorIp": "监听IP",
        "alarm": "存储报警信息",
        "ptzSpeed": "云台控制速度",
        "id": "国标ID",
        "registerTimeInterval": "注册间隔",
        "artifactId": "模块名称",
        "version": "版本",
        "project": "工程",
        "git_Revision": "GIT修订版本",
        "git_BRANCH": "GIT分支",
        "git_URL": "GIT地址",
        "build_DATE": "构建时间",
        "create_By": "作者",
        "git_Revision_SHORT": "GIT修订版本（短）",
        "build_Jdk": "构建用JDK",
      };
      console.log(key + ": " + nameData[key])

      if (nameData[key]) {
        return nameData[key]
      } else {
        return key;
      }
    },
    getMediaKeyNameFromKey: function (key) {
      let nameData = {
        "waitTrack": "等待编码信息",
        "interfaceAuthenticationExcludes": "不进行鉴权的接口",
        "playTimeout": "点播超时时间",
        "autoApplyPlay": "自动点播",
        "recordPushLive": "推流录像",
        "redisConfig": "自动配置redis",
        "thirdPartyGBIdReg": "stream信息正则",
        "savePositionHistory": "保存轨迹信息",
        "interfaceAuthentication": "接口鉴权",
        "serverId": "服务ID",
        "logInDatebase": "日志存储进数据库",
        "seniorSdp": "扩展SDP",
        "password": "密码",
        "port": "端口号",
        "keepaliveTimeOut": "心跳超时",
        "domain": "国标域",
        "ip": "IP地址",
        "monitorIp": "监听IP",
        "alarm": "存储报警信息",
        "ptzSpeed": "云台控制速度",
        "id": "国标ID",
        "registerTimeInterval": "注册间隔",
        "artifactId": "模块名称",
        "version": "版本",
        "project": "工程",
        "git_Revision": "GIT修订版本",
        "git_BRANCH": "GIT分支",
        "git_URL": "GIT地址",
        "build_DATE": "构建时间",
        "create_By": "作者",
        "git_Revision_SHORT": "GIT修订版本（短）",
        "build_Jdk": "构建用JDK",
      };
      console.log(key + ": " + nameData[key])

      if (nameData[key]) {
        return nameData[key]
      } else {
        return key;
      }
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
