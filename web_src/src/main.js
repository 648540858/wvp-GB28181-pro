import Vue from 'vue';
import App from './App.vue';
import ElementUI, {Notification} from 'element-ui';
import 'element-ui/lib/theme-chalk/index.css';
import router from './router/index.js';
import axios from 'axios';
import VueCookies from 'vue-cookies';
import VCharts from 'v-charts';
import logViewer from '@femessage/log-viewer';

import VueClipboard from 'vue-clipboard2';
import Fingerprint2 from 'fingerprintjs2';
import VueClipboards from 'vue-clipboards';
import Contextmenu from "vue-contextmenujs"
import userService from "./components/service/UserService"

Vue.config.productionTip = false;


// 生成唯一ID
Fingerprint2.get(function (components) {
  const values = components.map(function (component, index) {
    if (index === 0) { //把微信浏览器里UA的wifi或4G等网络替换成空,不然切换网络会ID不一样
      return component.value.replace(/\bNetType\/\w+\b/, '');
    }
    return component.value;
  })
  //console.log(values)  //使用的浏览器信息npm
  // 生成最终id
  let port = window.location.port;
  const fingerPrint = Fingerprint2.x64hash128(values.join(port), 31)
  Vue.prototype.$browserId = fingerPrint;
  console.log("浏览器 ID: " + fingerPrint);
});

Vue.use(VueClipboard);
Vue.use(ElementUI);
Vue.use(VueCookies);
Vue.use(VueClipboards);

Vue.prototype.$notify = Notification;
Vue.use(Contextmenu);
Vue.use(VCharts);
Vue.use(logViewer);

axios.defaults.baseURL = (process.env.NODE_ENV === 'development') ? process.env.BASE_API : (window.baseUrl ? window.baseUrl : "");
axios.defaults.withCredentials = true;
// api 返回401自动回登陆页面
axios.interceptors.response.use((response) => {
  // 对响应数据做点什么
  let token = response.headers["access-token"];
  if (token) {
    userService.setToken(token)
  }
  return response;
}, (error) => {
  // 对响应错误做点什么
  if (error.response.status === 401) {
    console.log("Received 401 Response")
    router.push('/login');
  }
  return Promise.reject(error);
});
axios.interceptors.request.use(
  config => {
    if (userService.getToken() != null && config.url !== "/api/user/login") {
      config.headers['access-token'] = `${userService.getToken()}`;
    }
    return config;
  },
  error => {
    return Promise.reject(error);
  }
);

Vue.prototype.$axios = axios;
Vue.prototype.$cookies.config(60 * 30);
Vue.prototype.$tableHeght = window.innerHeight - 170;
Vue.prototype.$channelTypeList = {
  1: {id: 1, name: "国标设备", style: {color: "#409eff", borderColor: "#b3d8ff"}},
  2: {id: 2, name: "推流设备", style: {color: "#67c23a", borderColor: "#c2e7b0"}},
  3: {id: 3, name: "拉流代理", style: {color: "#e6a23c", borderColor: "#f5dab1"}},
};





new Vue({
  beforeCreate: function () {
    // 获取本平台的服务ID
    axios({
      method: 'get',
      url: `/api/server/system/configInfo`,
    }).then( (res)=> {
      if (res.data.code === 0) {
        Vue.prototype.$myServerId = res.data.data.addOn.serverId;
      }
    }).catch( (error)=> {
    });
  },
  router: router,
  render: h => h(App),
}).$mount('#app')
