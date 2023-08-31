<template>
  <div id="app">
    <router-view></router-view>
  </div>
</template>

<script>
import  userService from './components/service/UserService'
export default {
  name: 'app',
  data(){
    return {
      isLogin: false,
      excludeLoginCheck: ["/play/wasm", "/play/rtc"],
      userInfo: { //保存用户信息
        nick: null,
        ulevel: null,
        uid: null,
        portrait: null
      }
    }
  },
  created() {
    if (userService.getToken() == null){
      console.log(22222)
      console.log(this.$route.path)
      try {
        if (this.excludeLoginCheck && this.excludeLoginCheck.length > 0) {
          for (let i = 0; i < this.excludeLoginCheck.length; i++) {
            if (this.$route.path.startsWith(this.excludeLoginCheck[i])){
              return;
            }
          }
        }
      }catch (e) {
        console.error(e)
      }
      //如果没有登录状态则跳转到登录页
      this.$router.push('/login');
    }
  },

  mounted(){
    //组件开始挂载时获取用户信息
    // this.getUserInfo();
  },
  methods: {
  },
  components: {}
};
</script>

<style>
html,
body,
#app {
  margin: 0 0;
  background-color: #e9eef3;
  height: 100%;
}
.el-header,
.el-footer {
  /* background-color: #b3c0d1; */
  color: #333;
  text-align: center;
  line-height: 60px;
}
.el-main {
  background-color: #f0f2f5;
  color: #333;
  text-align: center;
  padding-top: 0px !important;
}

/*定义滚动条高宽及背景 高宽分别对应横竖滚动条的尺寸*/
::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

/*定义滚动条轨道 内阴影+圆角*/
::-webkit-scrollbar-track {
  border-radius: 4px;
  background-color: #F5F5F5;
}

/*定义滑块 内阴影+圆角*/
::-webkit-scrollbar-thumb {
  border-radius: 4px;
  background-color: #c8c8c8;
  box-shadow: inset 0 0 6px rgba(0, 0, 0, .1);
  -webkit-box-shadow: inset 0 0 6px rgba(0, 0, 0, .1);
}
.table-header {
  color: #727272;
  font-weight: 600;
}
</style>
