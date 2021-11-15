<template>
  <a-modal
    title="修改密码"
    :width="900"
    :visible.sync="showDialog"
    :confirmLoading="loading"
    :destroy-on-close="true"
    @ok="handleOk"
    @cancel="cancelForm"
  >
    <a-spin :spinning="loading">
      <a-form-model ref="form" :model = "proxyParam" :layout="form.layout" :rules="rules" v-bind="formItemLayout">
        <a-form-model-item label="旧密码" prop="oldPassword">
          <a-input v-model="proxyParam.oldPassword" clearable></a-input>
        </a-form-model-item>
        <a-form-model-item label="新密码" prop="newPassword">
          <a-input v-model="proxyParam.newPassword" clearable></a-input>
        </a-form-model-item>
        <a-form-model-item label="确认密码" prop="confirmPassword">
          <a-input v-model="proxyParam.confirmPassword" clearable></a-input>
        </a-form-model-item>
      </a-form-model>
    </a-spin>
  </a-modal>
</template>

<script>

import {changePassword} from "@/api/login";
import crypto from 'crypto'
export default {
  props: {},
  data() {
    let validatePass0 = (rule, value, callback) => {
      if (value === '') {
        callback(new Error('请输入旧密码'));
      } else {
        callback();
      }
    };
    let validatePass1 = (rule, value, callback) => {
      if (value === '') {
        callback(new Error('请输入新密码'));
      } else {
        if (this.proxyParam.confirmPassword !== '') {
          this.$refs.form.validateField('confirmPassword');
        }
        callback();
      }
    };
    let validatePass2 = (rule, value, callback) => {
      if (this.proxyParam.confirmPassword === '') {
        callback(new Error('请再次输入密码'));
      } else if (this.proxyParam.confirmPassword !== this.proxyParam.newPassword) {
        callback(new Error('两次输入密码不一致!'));
      } else {
        callback();
      }
    };
    return {
      showDialog: false,
      loading: false,
      proxyParam: {
        oldPassword: null,
        newPassword: null,
        confirmPassword: null,
      },
      form: {
        layout: 'horizontal'
      },
      type: 'default',
      rules: {
        oldPassword: [{ required: true, validator: validatePass0, trigger: "blur" }],
        newPassword: [{ required: true, validator: validatePass1, trigger: "blur" }],
        confirmPassword: [{ required: true, validator: validatePass2, trigger: "blur" }],
      },
    }
  },
  computed: {
    formItemLayout() {
      const {layout} = this.form;
      return layout === 'horizontal'
        ? {
          labelCol: {
            xs: {span: 24},
            sm: {span: 7}
          },
          wrapperCol: {
            xs: {span: 24},
            sm: {span: 13}
          }
        }
        : {};
    },
  },
  methods: {
    open(proxyParam) {
      this.showDialog = true;
    },
    cancelForm() {
      console.log("关闭修改面");
      this.showDialog = false;
      const form = this.$refs.form
      form.resetFields()
    },
    handleOk() {
      this.confirmLoading = true
      console.log("form",this.$refs.form)
      this.$refs.form.validate(valid => {
        if (!valid) {
          this.confirmLoading = false
          return
        }

        const requestParameters = Object.assign({})
          requestParameters.oldPassword = crypto.createHash('md5').update(this.proxyParam.oldPassword, "utf8").digest('hex'),
          requestParameters.password = this.proxyParam.newPassword
        changePassword(requestParameters).then(res => {
          if (res === "success"){
            this.$message.success(
              '修改成功，请重新登录'
            );
            this.showDialog = false;
            setTimeout(()=>{
              // 删除cookie，回到登录页面
              this.$cookies.remove("session");
              this.$router.push('/login');
              // this.sseSource.close();
            },800)
          }else {
            this.$message.error(
              '修改密码失败，是否已登录（接口鉴权关闭无法修改密码）'
            );
          }
        }).catch(err => {
          console.log(err)
        })
      })
    },
  },
  mounted() {

  }
}
</script>