<template>
  <a-config-provider :locale="locale">
    <div id="app">
      <router-view/>
    </div>
  </a-config-provider>
</template>

<script>
import { domTitle, setDocumentTitle } from '@/utils/domUtil'
import { i18nRender } from '@/locales'

export default {
  data () {
    return {
      isLogin: false,
      userInfo: { //保存用户信息
        nick: null,
        ulevel: null,
        uid: null,
        portrait: null
      }
    }
  },
  created() {
    this.$router.beforeEach((to, from, next) => {
      console.log(to);
      if(!this.$cookies.get("session") && to.name != "login"){
        //如果没有登录状态则跳转到登录页
        this.$router.push('/login');
        next();
      }
      next();
    });
  },
  //监听路由检查登录
  watch:{
    // "$router" : 'checkLogin'
  },
  computed: {
    locale () {
      // 只是为了切换语言时，更新标题
      const { title } = this.$route.meta
      title && (setDocumentTitle(`${i18nRender(title)} - ${domTitle}`))

      return this.$i18n.getLocaleMessage(this.$store.getters.lang).antLocale
    },
    //请求用户的一些信息
    getUserInfo(){
      let userinfo = this.$cookies.get("session");
    },
    checkLogin(){
      //检查是否存在session
      //cookie操作方法在源码里有或者参考网上的即可
      if(!this.$cookies.get("session")){
        console.log(111)
        //如果没有登录状态则跳转到登录页
       setTimeout(() => {
         this.$router.push('/login');
       },500)
      }
    },
    getCookie: function (cname) {
      let name = cname + "=";
      let ca = document.cookie.split(';');
      for (let i = 0; i < ca.length; i++) {
        let c = ca[i];
        while (c.charAt(0) == ' ') c = c.substring(1);
        if (c.indexOf(name) != -1) return c.substring(name.length, c.length);
      }
      return "";
    }
  }
}
</script>
