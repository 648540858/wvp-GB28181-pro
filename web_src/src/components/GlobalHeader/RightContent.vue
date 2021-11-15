<template>
  <a-space size="middle" style="float: right;margin-right: 20px">
    <a-tooltip title="刷新页面">
      <a @click="() => $router.go(0)">
        <a-icon type="redo"/>
        刷新
      </a>
    </a-tooltip>
    <a-tooltip title="系统通知">
      <a @click="goWarningPage">
        <a-badge :dot="dotVisible">
          <a-icon type="bell"/>
        </a-badge>
        通知
      </a>
    </a-tooltip>
    <a-dropdown>
      <a @click="e => e.preventDefault()">
        <a-icon type="global"/>
        语言
      </a>
      <a-menu slot="overlay" @click="setLanguage">
        <a-menu-item key="zh-CN">
          <span role="img" aria-label="简体中文">🇨🇳</span> 简体中文
        </a-menu-item>
        <a-menu-item key="en-US">
          <span role="img" aria-label="English">🇺🇸</span> English
        </a-menu-item>
      </a-menu>
    </a-dropdown>

    <a-dropdown>
      <a @click="e => e.preventDefault()">
        欢迎
        <a-tag color="blue">
          {{username}}
        </a-tag>
      </a>
      <a-menu slot="overlay">
        <a-menu-item @click="update">
           修改密码
        </a-menu-item>
        <a-menu-item @click="loginout">
            注销
        </a-menu-item>
      </a-menu>
    </a-dropdown>
    <changePassword ref="changePassword"/>
  </a-space>

</template>

<script>
import changePassword from '@/components/GlobalHeader/changePassword.vue'
import i18nMixin from '@/store/i18n-mixin'
import {getWarningList} from "@/api/warning";
import {logout} from "@/api/login";
export default {
  name: 'RightContent',
  mixins: [i18nMixin],
  components: {
    changePassword
  },
  methods: {
    loginout(){
      return logout().then(res => {
        debugger;
        // 删除cookie，回到登录页面
        this.$cookies.remove("session");
        this.$router.push('/login');
      }).catch(error=>{
        console.error("登出失败")
        console.error(error)
      })

    },
    setLanguage({key}) {
      this.setLang(key)
    },
    goWarningPage(){
      this.$router.push({ path: '/deviceWarning'})
    },
    update(){
      this.$refs.changePassword.open(null);
    },
  },
  data() {
    return {
      dotVisible: false,
      username: null
    }
  },
  mounted() {
    this.username  = this.$cookies.get("session").username;
    getWarningList({pageNo: 1, pageSize: 10}).then(res => {
      if (res.data && res.data.data && res.data.data.length > 0) {
        this.dotVisible = true
      }
    })
  }
}
</script>
