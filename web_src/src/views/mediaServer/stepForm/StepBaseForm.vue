<template>
  <a-spin :spinning="loading">
    <a-form-model ref="form" :model="mediaServerConf" :layout="form.layout" :rules="rules" v-bind="formItemLayout">
      <a-form-model-item label="IP地址" prop="ip">
        <a-input v-model="mediaServerConf.ip" placeholder="媒体服务IP"></a-input>
      </a-form-model-item>
      <a-form-model-item label="HTTP端口" prop="httpPort">
        <a-input v-model="mediaServerConf.httpPort" placeholder="媒体服务HTTP端口"></a-input>
      </a-form-model-item>
      <a-form-model-item label="SECRET密钥" prop="secret">
        <a-input v-model="mediaServerConf.secret" placeholder="媒体服务SECRET"></a-input>
      </a-form-model-item>
      <a-form-model-item :wrapper-col="{ offset: 8 }">
        <a-button type="primary" @click="checkServer">
          测试
        </a-button>
        <a-button style="margin-left: 10px;" @click="nextStep">
          下一步
        </a-button>
        <span v-if="stepCheck" style="color: lightgreen"><a-icon type="check" style="margin-left: 20px"/> 测试通过，请点击下一步继续完善信息</span>
      </a-form-model-item>
    </a-form-model>
  </a-spin>
</template>

<script>
import {checkServer} from "@/api/mediaServer";

export default {
  name: "StepBaseInfo",
  props: ['mediaServerConf'],
  data() {
    let validateIp = (rule, value, callback) => {
      // 校验IP是否符合规则
      let reg = /^(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])\.(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])\.(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])\.(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])$/
      if (!reg.test(value)) {
        return callback(new Error('请输入有效的IP地址'))
      } else {
        callback()
      }
      return true
    }
    let validatePort = (rule, value, callback) => { // 校验port是否符合规则
      let reg = /^(([0-9]|[1-9]\d{1,3}|[1-5]\d{4}|6[0-5]{2}[0-3][0-5]))$/
      if (!reg.test(value)) {
        return callback(new Error('请输入有效的端口号'))
      } else {
        callback()
      }
      return true
    }
    return {
      loading: false,
      form: {
        layout: 'horizontal'
      },
      rules: {
        ip: [{required: true, validator: validateIp, message: '请输入有效的IP地址', trigger: 'blur'}],
        httpPort: [{required: true, validator: validatePort, message: '请输入有效的端口号', trigger: 'blur'}],
      },
      stepCheck: false
    }
  },
  created() {
    console.log(this.mediaServerConf)
  },
  computed: {
    formItemLayout() {
      const {layout} = this.form;
      return layout === 'horizontal'
        ? {
          labelCol: {
            xs: {span: 8},
            sm: {span: 8}
          },
          wrapperCol: {
            xs: {span: 8},
            sm: {span: 8}
          }
        }
        : {};
    }
  },
  methods: {
    checkServer() {
      this.loading = true
      this.$refs.form.validate(valid => {
        if (!valid) {
          this.loading = false
          return
        }
        let params = {
          ip: this.mediaServerConf.ip,
          port: this.mediaServerConf.httpPort,
          secret: this.mediaServerConf.secret
        }
        checkServer(params).then(res => {
          this.loading = false
          if (res.code === 0) {
            if (parseInt(this.mediaServerConf.httpPort) !== parseInt(res.data.httpPort)) {
              this.$message.warn('服务器端口：' + res.data.httpPort + '， 当前端口：' + this.mediaServerConf.httpPort + '，如果你正在使用docker部署你的媒体服务，请注意的端口映射');
            }
            let httpPort = this.mediaServerConf.httpPort;
            this.mediaServerConf = res.data;
            this.mediaServerConf.httpPort = httpPort;
            this.mediaServerConf.autoConfig = true;

            this.stepCheck = true //表示可以进行下一步操作了
          } else {
            this.$message.error(res.msg);
            this.stepCheck = false
          }
        })
      })
    },
    nextStep() {
      if (this.stepCheck) {
        this.$emit('nextStep', this.mediaServerConf)
      } else {
        this.$message.error('基本信息测试不通过，无法进行下一步')
      }
    }
  }
}
</script>

<style scoped>

</style>