<template>
  <div id="SettingForSip" style="width: 100%">
    <div class="page-header">
      <div class="page-title">国标服务</div>
    </div>
    <div
      style="width: 60%; margin:0 auto; background-color: #FFFFFF; position: relative; padding: 5rem 6.5rem; text-align: left;font-size: 14px; max-width: 400px">
      <el-form ref="form" :rules="rules" :model="form" label-width="140px">
        <el-form-item label="ip" prop="ip">
          <el-input v-model="form.ip" clearable></el-input>
        </el-form-item>
        <el-form-item label="端口" prop="port">
          <el-input v-model.number="form.port" clearable></el-input>
        </el-form-item>
        <el-form-item label="域" prop="domain">
          <el-input v-model="form.domain" clearable></el-input>
        </el-form-item>
        <el-form-item label="id" prop="id">
          <el-input v-model="form.id" clearable></el-input>
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" clearable></el-input>
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
  name: "SettingForSip",
  components: {
    uiHeader
  },
  data() {
    return {
      form: {
        ip: null,
        port: null,
        domain: null,
        id: null,
        password: null,
      },

      rules: {
        ip: [{required: true, message: "请输入名称", trigger: "blur"}],
        port: [{required: true, message: "请输入应用名", trigger: "blur"}],
        domain: [{required: true, message: "请输入流ID", trigger: "blur"}],
        id: [{required: true, message: "请输入国标编码", trigger: "blur"}],
        password: [{required: true, message: "请输入国标编码", trigger: "blur"}],
      },
    }
  }
}
</script>

<style scoped>

</style>
