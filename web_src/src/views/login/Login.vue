<template>
  <common-layout>
    <div class="top">
      <div class="header">
        <img alt="logo" class="logo" src="../../assets/logo.svg" />
        <span class="title">{{systemName}}</span>
      </div>
      <div class="desc">Ant Design 是西湖区最具影响力的 Web 设计规范</div>
    </div>
    <div class="login">
      <a-form @submit="onSubmit" :form="form">
        <a-tabs size="large" :tabBarStyle="{textAlign: 'center'}" style="padding: 0 2px;">
          <a-tab-pane tab="账户密码登录" key="1">
            <a-alert type="error" :closable="true" v-show="error" :message="error" showIcon style="margin-bottom: 24px;" />
            <a-form-item>
              <a-input
                autocomplete="autocomplete"
                size="large"
                placeholder="请输入账号"
                v-decorator="['name', {rules: [{ required: true, message: '请输入账户名', whitespace: true}]}]"
              >
                <a-icon slot="prefix" type="user" />
              </a-input>
            </a-form-item>
            <a-form-item>
              <a-input
                size="large"
                placeholder="请输入密码"
                autocomplete="autocomplete"
                type="password"
                v-decorator="['password', {rules: [{ required: true, message: '请输入密码', whitespace: true}]}]"
              >
                <a-icon slot="prefix" type="lock" />
              </a-input>
            </a-form-item>
          </a-tab-pane>
          <a-tab-pane tab="手机号登录" key="2" disabled="disabled">
            <a-form-item>
              <a-input size="large" placeholder="mobile number" >
                <a-icon slot="prefix" type="mobile" />
              </a-input>
            </a-form-item>
            <a-form-item>
              <a-row :gutter="8" style="margin: 0 -4px">
                <a-col :span="16">
                  <a-input size="large" placeholder="captcha">
                    <a-icon slot="prefix" type="mail" />
                  </a-input>
                </a-col>
                <a-col :span="8" style="padding-left: 4px">
                  <a-button style="width: 100%" class="captcha-button" size="large" disabled="disabled">获取验证码</a-button>
                </a-col>
              </a-row>
            </a-form-item>
          </a-tab-pane>
        </a-tabs>
        <div>
          <a-checkbox :checked="true" disabled="disabled">自动登录</a-checkbox>
          <a style="float: right" disabled="disabled">忘记密码</a>
        </div>
        <a-form-item>
          <a-button :loading="logging" style="width: 100%;margin-top: 24px" size="large" htmlType="submit" type="primary">登录</a-button>
        </a-form-item>
        <div>
          其他登录方式
          <a-icon class="icon" type="alipay-circle" disabled="disabled"/>
          <a-icon class="icon" type="taobao-circle" disabled="disabled"/>
          <a-icon class="icon" type="weibo-circle" disabled="disabled"/>
          <router-link style="float: right" to="/dashboard/workplace" disabled="disabled">注册账户</router-link>
        </div>
      </a-form>
    </div>
  </common-layout>
</template>

<script>
import crypto from 'crypto'
import CommonLayout from '@/layouts/CommonLayout'
import {mapMutations} from 'vuex'
import {toLogin} from "@/api/login";

export default {
  name: 'Login',
  components: {CommonLayout},
  data () {
    return {
      logging: false,
      error: '',
      form: this.$form.createForm(this)
    }
  },
  computed: {
    systemName () {
      return 'WVP管理平台'
    }
  },
  methods: {
    cancelEnterkeyDefaultAction: function() {
      document.onkeydown = function(e) {
        var key = window.event.keyCode;
        if (key == 13) {
          return false;
        }
      }
    },
    setCookie: function (cname, cvalue, exdays) {
      let d = new Date();
      d.setTime(d.getTime() + (exdays * 24 * 60 * 60 * 1000));
      let expires = "expires=" + d.toUTCString();
      console.info(cname + "=" + cvalue + "; " + expires);
      document.cookie = cname + "=" + cvalue + "; " + expires;
      console.info(document.cookie);
    },
    onSubmit (e) {
      let that = this;
      e.preventDefault()
      this.form.validateFields((err) => {
        if (!err) {
          this.logging = true
          const name = this.form.getFieldValue('name')
          const password = crypto.createHash('md5').update(this.form.getFieldValue('password'), "utf8").digest('hex')
          const requestParameters = Object.assign({})
          requestParameters.username = name
          requestParameters.password = password
          return toLogin(requestParameters).then(res => {
            debugger;
            console.log(JSON.stringify(res));
            if (res.code == 0 && res.msg == "success") {
              that.$cookies.set("session", {"username": res.data.username}) ;
              that.logging = false
              //登录成功后
              that.cancelEnterkeyDefaultAction();
              let router = that.$router;
              that.$router.push('/');
            }else{
              that.logging = false
              that.$message({
                showClose: true,
                message: '登录失败，用户名或密码错误',
                type: 'error'
              });

            }

          }).catch(error=>{
              console.log("res","登录失败")
          })
        }
      })
    },
  }
}
</script>

<style lang="less" scoped>
  .common-layout{
    .top {
      text-align: center;
      .header {
        height: 44px;
        line-height: 44px;
        a {
          text-decoration: none;
        }
        .logo {
          height: 44px;
          vertical-align: top;
          margin-right: 16px;
        }
        .title {
          font-size: 33px;
          color: rgba(0, 0, 0, 0.85);
          font-family: 'Myriad Pro', 'Helvetica Neue', Arial, Helvetica, sans-serif;
          font-weight: 600;
          position: relative;
          top: 2px;
        }
      }
      .desc {
        font-size: 14px;
        color: #909399;
        margin-top: 12px;
        margin-bottom: 40px;
      }
    }
    .login{
      width: 368px;
      margin: 0 auto;
      @media screen and (max-width: 576px) {
        width: 95%;
      }
      @media screen and (max-width: 320px) {
        .captcha-button{
          font-size: 14px;
        }
      }
      .icon {
        font-size: 24px;
        color: rgba(0, 0, 0, 0.45);
        margin-left: 16px;
        vertical-align: middle;
        cursor: pointer;
        transition: color 0.3s;

        &:hover {
          color: rgba(0, 0, 0, 0.45);
        }
      }
    }
  }
</style>
