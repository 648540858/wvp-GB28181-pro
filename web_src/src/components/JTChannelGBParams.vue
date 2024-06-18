<template>
  <div id="channelList" style="width: 100%">
    <div class="page-header">
      <div class="page-title">
        <el-button icon="el-icon-back" size="mini" style="font-size: 20px; color: #000;" type="text" @click="showDevice" ></el-button>
        <el-divider direction="vertical"></el-divider>
        国标通道参数
      </div>
      <div class="page-header-btn">
        <div style="display: inline;">
        <el-button icon="el-icon-close" circle size="mini" @click="showDevice()"></el-button>
        </div>
      </div>
    </div>
    <el-container v-loading="isLoading" style="height: 82vh; overflow: auto">
      <el-main style="padding: 5px; background-color: #ffffff;">
        <el-divider content-position="center">通讯参数</el-divider>
        <el-form size="mini"  ref="form" :rules="rules" :model="form" label-width="240px" style="display: grid; grid-template-columns: 1fr 1fr 1fr 1fr;">
          <el-form-item label="编码" prop="gbDeviceId">
            <el-input v-model="form.gbDeviceId" clearable></el-input>
          </el-form-item>
          <el-form-item label="名称" prop="gbName">
            <el-input v-model="form.gbName" clearable></el-input>
          </el-form-item>
          <el-form-item label="设备厂商" prop="gbManufacturer">
            <el-input v-model="form.gbManufacturer" clearable></el-input>
          </el-form-item>
          <el-form-item label="设备型号" prop="gbModel">
            <el-input v-model="form.gbModel" clearable></el-input>
          </el-form-item>
          <el-form-item label="行政区域" prop="gbCivilCode">
            <el-input v-model="form.gbCivilCode" clearable></el-input>
          </el-form-item>
          <el-form-item label="警区" prop="gbBlock">
            <el-input v-model="form.gbBlock" clearable></el-input>
          </el-form-item>
          <el-form-item label="安装地址" prop="gbAddress">
            <el-input v-model="form.gbAddress" clearable></el-input>
          </el-form-item>
          <el-form-item label="是否有子设备" prop="gbParental">
            <el-checkbox v-model="form.gbParental" ></el-checkbox>
          </el-form-item>
          <el-form-item label="父节点ID" prop="gbParentId">
            <el-input v-model="form.gbParentId" clearable></el-input>
          </el-form-item>
          <el-form-item label="注册方式" prop="gbRegisterWay">
            <el-input v-model="form.gbRegisterWay" clearable></el-input>
          </el-form-item>
          <el-form-item label="摄像机安全能力等级代码" prop="gbSecurityLevelCode">
            <el-input v-model="form.gbSecurityLevelCode" clearable></el-input>
          </el-form-item>
          <el-form-item label="保密属性" prop="gbSecrecy">
            <el-select v-model="form.gbSecrecy" style="float: left; width: 100%" >
              <el-option label="不涉密" :value="0"></el-option>
              <el-option label="涉密" :value="1"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="IP" prop="gbIpAddress">
            <el-input v-model="form.gbIpAddress" clearable></el-input>
          </el-form-item>
          <el-form-item label="端口" prop="gbPort">
            <el-input v-model="form.gbPort" clearable></el-input>
          </el-form-item>
          <el-form-item label="口令" prop="gbPort">
            <el-input v-model="form.gbPort" clearable></el-input>
          </el-form-item>
          <el-form-item label="状态" prop="gbStatus">
            <el-switch
              v-model="form.gbStatus"
              active-text="在线"
              inactive-text="离线">
            </el-switch>
          </el-form-item>
          <el-form-item label="经度（WGS-84坐标系）" prop="gbLongitude">
            <el-input v-model="form.gbLongitude" clearable></el-input>
          </el-form-item>
          <el-form-item label="纬度（WGS-84坐标系）" prop="gbLatitude">
            <el-input v-model="form.gbLatitude" clearable></el-input>
          </el-form-item>
          <el-form-item label="虚拟组织所属的业务分组ID" prop="gbBusinessGroupId">
            <el-input v-model="form.gbBusinessGroupId" clearable></el-input>
          </el-form-item>

          <el-form-item label="摄像机结构类型" prop="gbPtzType">
            <el-select v-model="form.gbPtzType" style="float: left; width: 100%" >
              <el-option label="球机" :value="1"></el-option>
              <el-option label="半球" :value="2"></el-option>
              <el-option label="固定枪机" :value="3"></el-option>
              <el-option label="遥控枪机" :value="4"></el-option>
              <el-option label="遥控半球" :value="5"></el-option>
              <el-option label="多目设备的全景/拼接通道" :value="6"></el-option>
              <el-option label="多目设备的分割通道" :value="7"></el-option>
            </el-select>
          </el-form-item>

          <el-form-item label="摄像机光电成像类型" prop="gbPtzType">
            <el-select multiple v-model="form.gbPtzType" style="float: left; width: 100%" >
              <el-option label="可见光成像" :value="1"></el-option>
              <el-option label="热成像" :value="2"></el-option>
              <el-option label="雷达成像" :value="3"></el-option>
              <el-option label="X光成像" :value="4"></el-option>
              <el-option label="深度光场成像" :value="5"></el-option>
              <el-option label="其他" :value="6"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="摄像机采集部位类型" prop="gbCapturePositionType">
            <el-input v-model="form.gbCapturePositionType" clearable></el-input>
          </el-form-item>
          <el-form-item label="室外/室内" prop="gbRoomType">
            <el-select multiple v-model="form.gbRoomType" style="float: left; width: 100%" >
              <el-option label="室外" :value="1"></el-option>
              <el-option label="室内" :value="2"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="摄像机补光属性" prop="gbSupplyLightType">
            <el-select multiple v-model="form.gbSupplyLightType" style="float: left; width: 100%" >
              <el-option label="无补光" :value="1"></el-option>
              <el-option label="红外补光" :value="2"></el-option>
              <el-option label="白光补光" :value="3"></el-option>
              <el-option label="激光补光" :value="4"></el-option>
              <el-option label="其他" :value="9"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="摄像机监视方位" prop="udpPortIcMaster">
            <el-select multiple v-model="form.gbRoomType" style="float: left; width: 100%" >
              <el-option label="东(西向东)" :value="1"></el-option>
              <el-option label="西(东向西)" :value="2"></el-option>
              <el-option label="南(北向南)" :value="3"></el-option>
              <el-option label="北(南向北)" :value="4"></el-option>
              <el-option label="东南(西北到东南)" :value="5"></el-option>
              <el-option label="东北(西南到东北)" :value="6"></el-option>
              <el-option label="西南(东北到西南)" :value="7"></el-option>
              <el-option label="西北(东南到西北)" :value="8"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="摄像机支持的分辨率" prop="gbResolution">
            <el-input v-model="form.gbResolution" clearable></el-input>
          </el-form-item>
          <el-form-item label="摄像机支持的码流编号列表" prop="gbStreamNumberList">
            <el-input v-model="form.gbStreamNumberList" clearable></el-input>
          </el-form-item>
          <el-form-item label="下载倍速" prop="gbDownloadSpeed">
            <el-input v-model="form.gbDownloadSpeed" clearable></el-input>
          </el-form-item>
          <el-form-item label="空域编码能力" prop="gbSvcSpaceSupportMod">
            <el-select multiple v-model="form.gbSvcSpaceSupportMod" style="float: left; width: 100%" >
              <el-option label="不支持" :value="0"></el-option>
              <el-option label="1级增强(1个增强层)" :value="1"></el-option>
              <el-option label="2级增强(2个增强层)" :value="2"></el-option>
              <el-option label="3级增强(3个增强层)" :value="3"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="时域编码能力" prop="gbSvcTimeSupportMode">
            <el-select multiple v-model="form.gbSvcTimeSupportMode" style="float: left; width: 100%" >
              <el-option label="不支持" :value="0"></el-option>
              <el-option label="1级增强" :value="1"></el-option>
              <el-option label="2级增强" :value="2"></el-option>
              <el-option label="3级增强" :value="3"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="SSVC增强层与基本层比例能力" prop="gbSsvcRatioSupportList">
            <el-input v-model="form.gbSsvcRatioSupportList" clearable></el-input>
          </el-form-item>
          <el-form-item label="移动采集设备类型" prop="gbMobileDeviceType">
            <el-select multiple v-model="form.gbMobileDeviceType" style="float: left; width: 100%" >
              <el-option label="移动机器人载摄像机" :value="1"></el-option>
              <el-option label="执法记录仪" :value="2"></el-option>
              <el-option label="移动单兵设备" :value="3"></el-option>
              <el-option label="车载视频记录设备" :value="4"></el-option>
              <el-option label="无人机载摄像机" :value="5"></el-option>
              <el-option label="其他" :value="9"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="休眠时汇报距离间隔(米)" prop="reportingDistanceDormancy">
            <el-input v-model="form.reportingDistanceDormancy" clearable></el-input>
          </el-form-item>
          <el-form-item label="紧急报警时汇报距离间隔(米)" prop="reportingDistanceEmergencyAlarm">
            <el-input v-model="form.reportingDistanceEmergencyAlarm" clearable></el-input>
          </el-form-item>
          <el-form-item label="拐点补传角度(度，小于180)" prop="inflectionPointAngle">
            <el-input v-model="form.inflectionPointAngle" clearable></el-input>
          </el-form-item>
        </el-form>
        <el-divider content-position="center">电话号码</el-divider>
        <el-form size="mini"  ref="form" :rules="rules" :model="form" label-width="240px" style="display: grid; grid-template-columns: 1fr 1fr 1fr 1fr;">
          <el-form-item label="监控平台电话号码" prop="platformPhoneNumber">
            <el-input v-model="form.platformPhoneNumber" clearable></el-input>
          </el-form-item>
          <el-form-item label="复位电话号码" prop="phoneNumberForFactoryReset">
            <el-input v-model="form.phoneNumberForFactoryReset" clearable></el-input>
          </el-form-item>
          <el-form-item label="监控平台SMS电话号码" prop="phoneNumberForSms">
            <el-input v-model="form.phoneNumberForSms" clearable></el-input>
          </el-form-item>
          <el-form-item label="接收终端SMS文本报警号码" prop="phoneNumberForReceiveTextAlarm">
            <el-input v-model="form.phoneNumberForReceiveTextAlarm" clearable></el-input>
          </el-form-item>
          <el-form-item label="终端电话接听策略" prop="locationReportingStrategy">
            <el-select v-model="form.locationReportingStrategy" style="float: left; width: 100%" >
              <el-option label="自动接听" :value="0">定时汇报</el-option>
              <el-option label="ACC ON时自动接听 ,OFF时手动接听" :value="1"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="每次最长通话时间(秒)" prop="longestCallTimeForPerSession">
            <el-input v-model="form.longestCallTimeForPerSession" clearable></el-input>
          </el-form-item>
          <el-form-item label="当月最长通话时间(秒)" prop="longestCallTimeInMonth">
            <el-input v-model="form.longestCallTimeInMonth" clearable></el-input>
          </el-form-item>
          <el-form-item label="监听电话号码" prop="phoneNumbersForListen">
            <el-input v-model="form.phoneNumbersForListen" clearable></el-input>
          </el-form-item>
          <el-form-item label="监管平台特权短信号码" prop="privilegedSMSNumber">
            <el-input v-model="form.privilegedSMSNumber" clearable></el-input>
          </el-form-item>
        </el-form>

        <el-divider content-position="center">报警参数</el-divider>
        <el-form size="mini"  ref="form" :rules="rules" :model="form" label-width="240px" style="display: grid; grid-template-columns: 1fr 1fr 1fr 1fr;">
          <el-form-item label="报警屏蔽字(TODO)" prop="alarmMaskingWord">
            <el-input v-model="form.alarmMaskingWord" clearable></el-input>
          </el-form-item>
          <el-form-item label="报警发送文本 SMS 开关(TODO)" prop="alarmSendsTextSmsSwitch">
            <el-input v-model="form.alarmSendsTextSmsSwitch" clearable></el-input>
          </el-form-item>
          <el-form-item label="报警拍摄开关(TODO)" prop="alarmShootingSwitch">
            <el-input v-model="form.alarmShootingSwitch" clearable></el-input>
          </el-form-item>
          <el-form-item label="报警拍摄存储标志(TODO)" prop="alarmShootingStorageFlags">
            <el-input v-model="form.alarmShootingStorageFlags" clearable></el-input>
          </el-form-item>
          <el-form-item label="关键标志(TODO)" prop="KeySign">
            <el-input v-model="form.KeySign" clearable></el-input>
          </el-form-item>
          <el-form-item label="电子围栏半径(米)" prop="fenceRadius">
            <el-input v-model="form.fenceRadius" clearable></el-input>
          </el-form-item>
        </el-form>

        <el-divider content-position="center">行驶参数</el-divider>
        <el-form size="mini"  ref="form" :rules="rules" :model="form" label-width="240px" style="display: grid; grid-template-columns: 1fr 1fr 1fr 1fr;">
          <el-form-item v-if="form.illegalDrivingPeriods" label="违规行驶时段-开始时间(HH:mm)" prop="illegalDrivingPeriods">
            <el-input v-model="form.illegalDrivingPeriods.startTime" clearable></el-input>
          </el-form-item>
          <el-form-item v-if="form.illegalDrivingPeriods" label="违规行驶时段-结束时间(HH:mm)" prop="illegalDrivingPeriods">
            <el-input v-model="form.illegalDrivingPeriods.endTime" clearable></el-input>
          </el-form-item>
          <el-form-item label="最高速度(千米每小时)" prop="topSpeed">
            <el-input v-model="form.topSpeed" clearable></el-input>
          </el-form-item>
          <el-form-item label="超速持续时间(秒)" prop="overSpeedDuration">
            <el-input v-model="form.overSpeedDuration" clearable></el-input>
          </el-form-item>
          <el-form-item label="连续驾驶时间门限(秒)" prop="continuousDrivingTimeThreshold">
            <el-input v-model="form.continuousDrivingTimeThreshold" clearable></el-input>
          </el-form-item>
          <el-form-item label="当天累计驾驶时间门限(秒)" prop="cumulativeDrivingTimeThresholdForTheDay">
            <el-input v-model="form.cumulativeDrivingTimeThresholdForTheDay" clearable></el-input>
          </el-form-item>
          <el-form-item label="最小休息时间(秒)" prop="minimumBreakTime">
            <el-input v-model="form.minimumBreakTime" clearable></el-input>
          </el-form-item>
          <el-form-item label="最长停车时间(秒)" prop="maximumParkingTime">
            <el-input v-model="form.maximumParkingTime" clearable></el-input>
          </el-form-item>
          <el-form-item label="超速预警差值(1/10 千米每小时)" prop="overSpeedWarningDifference">
            <el-input v-model="form.overSpeedWarningDifference" clearable></el-input>
          </el-form-item>
          <el-form-item label="疲劳驾驶预警差值(秒)" prop="drowsyDrivingWarningDifference">
            <el-input v-model="form.drowsyDrivingWarningDifference" clearable></el-input>
          </el-form-item>
          <el-form-item label="碰撞报警-碰撞时间(毫秒)" prop="collisionAlarmParamsCollisionAlarmTime">
            <el-input v-model="form.collisionAlarmParams.collisionAlarmTime" clearable></el-input>
          </el-form-item>
          <el-form-item label="碰撞报警-碰撞加速度(0.1g)" prop="collisionAlarmParamsCollisionAcceleration">
            <el-input v-model="form.collisionAlarmParams.collisionAcceleration" clearable></el-input>
          </el-form-item>
          <el-form-item label="侧翻报警参数-侧翻角度(度)" prop="rolloverAlarm">
            <el-input v-model="form.rolloverAlarm" clearable></el-input>
          </el-form-item>
        </el-form>

        <el-divider content-position="center">定时拍照控制</el-divider>
        <el-form size="mini"  ref="form" :rules="rules" :model="form" label-width="240px" style="display: grid; grid-template-columns: 1fr 1fr 1fr 1fr;">
          <el-form-item label="定时拍照开关-通道1" prop="rolloverAlarm">
            <el-switch v-model="form.cameraTimer.switchForChannel1" ></el-switch>
          </el-form-item>
          <el-form-item label="定时拍照开关-通道2" prop="rolloverAlarm">
            <el-switch v-model="form.cameraTimer.switchForChannel2" ></el-switch>
          </el-form-item>
          <el-form-item label="定时拍照开关-通道3" prop="rolloverAlarm">
            <el-switch v-model="form.cameraTimer.switchForChannel3" ></el-switch>
          </el-form-item>
          <el-form-item label="定时拍照开关-通道4" prop="rolloverAlarm">
            <el-switch v-model="form.cameraTimer.switchForChannel4" ></el-switch>
          </el-form-item>
          <el-form-item label="定时拍照开关-通道5" prop="rolloverAlarm">
            <el-switch v-model="form.cameraTimer.switchForChannel5" ></el-switch>
          </el-form-item>
          <el-form-item label="定时拍照存储-通道1" prop="rolloverAlarm">
            <el-switch v-model="form.cameraTimer.storageFlagsForChannel1" ></el-switch>
          </el-form-item>
          <el-form-item label="定时拍照存储-通道2" prop="rolloverAlarm">
            <el-switch v-model="form.cameraTimer.storageFlagsForChannel2" ></el-switch>
          </el-form-item>
          <el-form-item label="定时拍照存储-通道3" prop="rolloverAlarm">
            <el-switch v-model="form.cameraTimer.storageFlagsForChannel3" ></el-switch>
          </el-form-item>
          <el-form-item label="定时拍照存储-通道4" prop="rolloverAlarm">
            <el-switch v-model="form.cameraTimer.storageFlagsForChannel4" ></el-switch>
          </el-form-item>
          <el-form-item label="定时拍照存储-通道5" prop="rolloverAlarm">
            <el-switch v-model="form.cameraTimer.storageFlagsForChannel5" ></el-switch>
          </el-form-item>

          <el-form-item label="定时时间间隔" prop="timeInterval">
            <el-input v-model="form.timeInterval" clearable></el-input>
          </el-form-item>
          <el-form-item label="定时时间单位" prop="rolloverAlarm">
            <el-switch v-model="form.cameraTimer.timeUnit" active-text="分" inactive-text="秒"></el-switch>
          </el-form-item>
        </el-form>
        <div style="float: right;">
          <el-button type="primary" @click="onSubmit" >确认</el-button>
          <el-button @click="showDevice">取消</el-button>
        </div>
      </el-main>
    </el-container>
  </div>
</template>

<script>
import devicePlayer from './dialog/jtDevicePlayer.vue'
import uiHeader from '../layout/UiHeader.vue'
import DeviceTree from "./common/DeviceTree";
import channelEdit from "./dialog/jtChannelEdit.vue";
import JTDeviceService from "./service/JTDeviceService";

export default {
  name: 'channelList',
  components: {
    channelEdit,
    devicePlayer,
    uiHeader,
    DeviceTree
  },
  data() {
    return {
      phoneNumber: this.$route.params.phoneNumber,
      form: {
        collisionAlarmParams: {},
        illegalDrivingPeriods: {},
        cameraTimer: {},
      },
      version: 3,
      rules: {
        deviceId: [{ required: true, message: "请输入设备编号", trigger: "blur" }]
      },
      winHeight: window.innerHeight - 200,
      beforeUrl: "/jtDeviceList",
      isLoading: false,
      loadSnap: {},
    };
  },

  mounted() {
    this.initData();
  },
  methods: {
    initData: function () {
      this.isLoading = true;
      this.$axios({
        method: 'get',
        url: `/api/jt1078/config`,
        params: {
          phoneNumber: this.phoneNumber
        }
      }).then((res)=> {
        this.isLoading = false;
        console.log(res)
        this.form = res.data.data;
      }).cache((e)=>{
        this.isLoading = false;
      });
    },
    onSubmit: function () {
      this.$axios({
        method: 'post',
        url: `/api/jt1078/set-config`,
        data: {
          phoneNumber: this.phoneNumber,
          config: this.form
        }
      }).then(function (res) {
        console.log(JSON.stringify(res));
      });
    },
    showDevice: function () {
      this.$router.push(this.beforeUrl)
    },
  }
};
</script>
