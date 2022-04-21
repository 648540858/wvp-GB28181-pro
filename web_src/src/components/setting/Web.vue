<template>
  <div id="SettingForWeb" style="width: 100%">
      <div class="page-header">
        <div class="page-title">WEB服务</div>
      </div>
      <div style="width: 60%; margin:0 auto; background-color: #FFFFFF; position: relative; padding: 5rem 6.5rem; text-align: left;font-size: 14px; max-width: 400px">
        <el-form ref="form" :rules="rules" :model="form" label-width="140px">
          <el-form-item label="HTTP端口" prop="port">
            <el-input v-model.number="form.port" clearable></el-input>
          </el-form-item>
          <el-form-item label="点播超时" >
            <el-input v-model.number="form.playTimeout" clearable></el-input>
          </el-form-item>
          <el-form-item label="自动点播" >
            <el-switch v-model="form.autoApplyPlay"></el-switch>
          </el-form-item>
          <el-form-item label="扩展SDP" >
            <el-switch v-model="form.seniorSdp"></el-switch>
          </el-form-item>
          <el-form-item label="保存轨迹" >
            <el-switch v-model="form.savePositionHistory"></el-switch>
          </el-form-item>
          <el-form-item label="等待音视频编码信息">
            <el-switch v-model="form.waitTrack"></el-switch>
          </el-form-item>
          <el-form-item label="开启接口鉴权">
            <el-switch v-model="form.interfaceAuthentication"></el-switch>
          </el-form-item>

          <el-form-item>
            <div style="float: right;">
              <el-button type="primary" @click="onSubmit">保存</el-button>
<!--              <el-button @click="close">取消</el-button>-->
            </div>

          </el-form-item>
        </el-form>
      </div>
  </div>
</template>

<script>
import uiHeader from '../../layout/UiHeader.vue'
export default {
  name: "SettingForWeb",
  components: {
    uiHeader
  },
  data() {
    return {
      form: {
        port: null,
        autoApplyPlay: null,
        seniorSdp: null,
        savePositionHistory: null,
        playTimeout: null,
        waitTrack: null,
        interfaceAuthentication: null,
      },

      rules: {
        name: [{ required: true, message: "请输入名称", trigger: "blur" }],
        app: [{ required: true, message: "请输入应用名", trigger: "blur" }],
        stream: [{ required: true, message: "请输入流ID", trigger: "blur" }],
        gbId: [{ required: true, message: "请输入国标编码", trigger: "blur" }],
      },
    }
  }
}
</script>

<style scoped>

</style>
