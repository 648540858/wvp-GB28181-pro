<template>
  <div id="ChannelEdit" v-loading="locading" style="width: 100%">
    <el-form ref="passwordForm" status-icon label-width="160px" style="padding-top: 1rem">
      <el-form-item label="名称" >
        <el-input v-if="form.name" v-model="form.gbName" :placeholder="form.name"></el-input>
        <el-input v-if="!form.name" v-model="form.gbName" placeholder="请输入通道名称"></el-input>
      </el-form-item>
      <el-form-item label="编码" >
        <el-input v-if="form.deviceId" v-model="form.gbDeviceId" :placeholder="form.deviceId"></el-input>
        <el-input v-if="!form.deviceId" v-model="form.gbDeviceId" placeholder="请输入通道编码"></el-input>
      </el-form-item>
      <el-form-item label="设备厂商" >
        <el-input v-if="form.manufacturer" v-model="form.gbManufacturer" :placeholder="form.manufacturer"></el-input>
        <el-input v-if="!form.manufacturer" v-model="form.gbManufacturer" placeholder="请输入设备厂商"></el-input>
      </el-form-item>
      <el-form-item label="设备型号" >
        <el-input v-if="form.model" v-model="form.gbModel" :placeholder="form.model"></el-input>
        <el-input v-if="!form.model" v-model="form.gbModel" placeholder="请输入设备型号"></el-input>
      </el-form-item>
      <el-form-item label="设备归属" >
        <el-input v-if="form.owner" v-model="form.gbOwner" :placeholder="form.owner"></el-input>
        <el-input v-if="!form.owner" v-model="form.gbOwner" placeholder="请输入设备归属"></el-input>
      </el-form-item>
      <el-form-item label="行政区域" >
        <el-input v-if="form.civilCode" v-model="form.gbCivilCode" :placeholder="form.civilCode"></el-input>
        <el-input v-if="!form.civilCode" v-model="form.gbCivilCode" placeholder="请输入行政区域"></el-input>
      </el-form-item>
      <el-form-item label="警区" >
        <el-input v-if="form.block" v-model="form.gbBlock" :placeholder="form.block"></el-input>
        <el-input v-if="!form.block" v-model="form.gbBlock" placeholder="请输入警区"></el-input>
      </el-form-item>
      <el-form-item label="安装地址" >
        <el-input v-if="form.address" v-model="form.gbAddress" :placeholder="form.address"></el-input>
        <el-input v-if="!form.address" v-model="form.gbAddress" placeholder="请输入安装地址"></el-input>
      </el-form-item>
      <el-form-item label="子设备" >
        <el-select v-model="form.gbParental" style="width: 100%" placeholder="请选择">
          <el-option label="有" :value="1"></el-option>
          <el-option label="无" :value="0"></el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="父节点编码" >
        <el-input v-if="form.parentId" v-model="form.gbParentId" :placeholder="form.parentId"></el-input>
        <el-input v-if="!form.parentId" v-model="form.gbParentId" placeholder="请输入父节点编码"></el-input>
      </el-form-item>
      <el-form-item label="信令安全模式" >
        <el-select v-model="form.gbSafetyWay" style="width: 100%" placeholder="请选择">
          <el-option label="不采用" :value="0"></el-option>
          <el-option label="S/MIME签名" :value="2"></el-option>
          <el-option label="S/MIME加密签名同时采用" :value="3"></el-option>
          <el-option label="数字摘要" :value="4"></el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="注册方式" >
        <el-select v-model="form.gbRegisterWay" style="width: 100%" placeholder="请选择">
          <el-option label="IETFRFC3261标准" :value="1"></el-option>
          <el-option label="基于口令的双向认证" :value="2"></el-option>
          <el-option label="基于数字证书的双向认证注册" :value="3"></el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="证书序列号" >
        <el-input type="number" v-if="form.certNum" v-model="form.gbCertNum" :placeholder="form.certNum"></el-input>
        <el-input type="number" v-if="!form.certNum" v-model="form.gbCertNum" placeholder="请输入证书序列号"></el-input>
      </el-form-item>
      <el-form-item label="证书有效标识" >
        <el-select v-model="form.gbCertifiable" style="width: 100%" placeholder="请选择">
          <el-option label="有效" :value="1"></el-option>
          <el-option label="无效" :value="0"></el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="无效原因码" >
        <el-input type="errCode" v-if="form.gbErrCode" v-model="form.gbCertNum" :placeholder="form.errCode"></el-input>
        <el-input type="errCode" v-if="!form.gbErrCode" v-model="form.gbCertNum" placeholder="请输入无效原因码"></el-input>
      </el-form-item>
      <el-form-item label="证书终止有效期" >
        <el-date-picker
          v-model="form.gbEndTime"
          type="datetime"
          placeholder="选择日期时间">
        </el-date-picker>
      </el-form-item>

      <div style="float: right;">
        <el-button type="primary" size="mini" @click="onSubmit">保存</el-button>
        <el-button size="mini" @click="close">取消</el-button>
      </div>
    </el-form>
  </div>
</template>

<script>

export default {
  name: "channelEdit",
  props: [ 'id',],
  computed: {},
  created() {
    // 获取完整信息
    this.getCommonChannel(data=>{
      if (data.gbDeviceDbId) {
        // 国标类型特殊处理
        this.getDeviceChannel(chanel=>{
          this.form = chanel;
          console.log(chanel)
        })
      }else {
        this.form = data;
        console.log(data)
      }
    })
  },
  data() {
    return {
      locading: true,
      form: {
        gbName: "测试",
        gbNameLabel: "测试"
      },
    };
  },
  methods: {
    onSubmit: function () {

    },
    close: function () {

    },
    getCommonChannel:function (callback) {
      this.$axios({
        method: 'get',
        url: "/api/common/channel/one",
        params: {
          id: this.id
        }
      }).then((res) => {
        if (res.data.code === 0) {
          if(callback) {
            callback(res.data.data)
          }
        }
      }).catch((error) => {
        console.error(error)
      }).finally(()=>[
        this.locading = false
      ])
    },
    getDeviceChannel:function (callback) {
      this.$axios({
        method: 'get',
        url: "/api/device/query/channel/raw",
        params: {
          id: this.id
        }
      }).then((res) => {
        if (res.data.code === 0) {
          if(callback) {
            callback(res.data.data)
          }
        }
      }).catch((error) => {
        console.error(error)
      }).finally(()=>[
        this.locading = false
      ])
    }
  },
};
</script>
