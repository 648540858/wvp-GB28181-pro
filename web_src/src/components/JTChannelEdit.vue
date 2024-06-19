<template>
  <div id="channelList" style="width: 100%">
    <div class="page-header">
      <div class="page-title">
        <el-button icon="el-icon-back" size="mini" style="font-size: 20px; color: #000;" type="text" @click="showDevice" ></el-button>
        <el-divider direction="vertical"></el-divider>
        通道编辑
      </div>
      <div class="page-header-btn">
        <div style="display: inline;">
        <el-button icon="el-icon-close" circle size="mini" @click="showDevice()"></el-button>
        </div>
      </div>
    </div>
    <el-container v-loading="isLoading" style="height: 82vh; overflow: auto">
      <el-main style="padding: 5px; background-color: #ffffff;">
        <el-divider content-position="center">部标通道参数</el-divider>
        <el-form ref="form" :rules="rules" :model="form" label-width="240px" style="display: grid; grid-template-columns: 1fr 1fr 1fr ">
          <el-form-item label="编号" prop="channelId">
            <el-input v-model="form.channelId" clearable></el-input>
          </el-form-item>
          <el-form-item label="名称" prop="name">
            <el-input v-model="form.name" clearable></el-input>
          </el-form-item>
        </el-form>
        <el-divider content-position="center">国标通道参数</el-divider>
        <el-form size="mini"  ref="form" :rules="rules" :model="form" label-width="240px" style="display: grid; grid-template-columns: 1fr 1fr 1fr ">
          <el-form-item label="国标编码" prop="gbDeviceId">
            <el-input v-model="form.gbDeviceId" clearable></el-input>
          </el-form-item>
          <el-form-item label="通道名称" prop="gbName">
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
          <el-form-item label="摄像机水平视场角(0-360)" prop="gbHorizontalFieldAngle">
            <el-input v-model="form.gbHorizontalFieldAngle" clearable></el-input>
          </el-form-item>
          <el-form-item label="摄像机竖直视场角(0-360)" prop="gbVerticalFieldAngle">
            <el-input v-model="form.gbVerticalFieldAngle" clearable></el-input>
          </el-form-item>
          <el-form-item label="摄像机可视距离(米)" prop="gbMaxViewDistance">
            <el-input v-model="form.gbMaxViewDistance" clearable></el-input>
          </el-form-item>
          <el-form-item label="基层组织编码" prop="gbGrassrootsCode">
            <el-input v-model="form.gbGrassrootsCode" clearable></el-input>
          </el-form-item>
          <el-form-item label="监控点位类型" prop="gbPoType">
            <el-select v-model="form.gbPoType" style="float: left; width: 100%" >
              <el-option label="一类视频监控点" :value="1"></el-option>
              <el-option label="二类视频监控点" :value="2"></el-option>
              <el-option label="三类视频监控点" :value="3"></el-option>
              <el-option label="其他点位" :value="4"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="MAC地址(XX-XX-XX-XX-XX-XX)" prop="gbMac">
            <el-input v-model="form.gbMac" clearable></el-input>
          </el-form-item>
          <el-form-item label="卡口功能类型" prop="gbFunctionType">
            <el-select v-model="form.gbFunctionType" style="float: left; width: 100%" >
              <el-option label="人脸卡口" value="01"></el-option>
              <el-option label="人员卡口" value="02"></el-option>
              <el-option label="机动车卡口" value="03"></el-option>
              <el-option label="非机动车卡口" value="04"></el-option>
              <el-option label="物品卡口" value="05"></el-option>
              <el-option label="其他" value="99"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="摄像机视频编码格式" prop="gbEncodeType">
            <el-input v-model="form.gbEncodeType" clearable></el-input>
          </el-form-item>
          <el-form-item label="摄像机安装使用时间" prop="gbInstallTime">
            <el-input v-model="form.gbInstallTime" clearable></el-input>
          </el-form-item>
          <el-form-item label="管理单位名称" prop="gbManagementUnit">
            <el-input v-model="form.gbManagementUnit" clearable></el-input>
          </el-form-item>
          <el-form-item label="管理单位联系人联系方式" prop="gbContactInfo">
            <el-input v-model="form.gbContactInfo" clearable></el-input>
          </el-form-item>
          <el-form-item label="录像保存天数" prop="gbRecordSaveDays">
            <el-input v-model="form.gbRecordSaveDays" clearable></el-input>
          </el-form-item>
          <el-form-item label="国民经济行业分类代码" prop="gbIndustrialClassification">
            <el-input v-model="form.gbIndustrialClassification" clearable></el-input>
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
      form: {
        id: this.$route.params.id,
        terminalDbId: this.$route.params.terminalDbId
      },
      version: 3,
      rules: {
        deviceId: [{ required: true, message: "请输入设备编号", trigger: "blur" }]
      },
      winHeight: window.innerHeight - 200,
      isLoading: false,
      loadSnap: {},
    };
  },

  mounted() {
    this.initData();
  },
  methods: {
    initData: function () {
      console.log(this.form.id)
      if (this.form.id) {
        this.isLoading = true;
        this.$axios({
          method: 'get',
          url: `/api/jt1078/terminal/channel/one`,
          params: {
            id: this.form.id
          }
        }).then((res)=> {
          this.isLoading = false;
          if (res.data.data) {
            this.form = res.data.data;
          }
        }).cache((e)=>{
          this.isLoading = false;
        });
      }else {
        isLoading = false;
      }
    },
    onSubmit: function () {
      console.log("onSubmit");
      let isEdit = typeof (this.form.id) !== "undefined"
      this.$axios({
        method: 'post',
        url:`/api/jt1078/terminal/channel/${isEdit?'update':'add'}/`,
        params: this.form
      }).then((res) => {
        console.log(res.data)
        if (res.data.code === 0) {
          this.$message({
            showClose: true,
            message: "保存成功",
            type: "success",
          });
        }else {
          this.$message({
            showClose: true,
            message: res.data.msg,
            type: "error",
          });
        }
      }).catch(function (error) {
        console.log(error);
      });
    },
    showDevice: function () {
      window.history.go(-1)
    },
  }
};
</script>
