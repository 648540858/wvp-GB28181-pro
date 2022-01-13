import Vue from 'vue';
import App from './App.vue';
Vue.config.productionTip = false;
import ElementUI from 'element-ui';
import 'element-ui/lib/theme-chalk/index.css';
import router from './router/index.js';
import axios from 'axios';
import VueCookies from 'vue-cookies';
import echarts from 'echarts';

import VueClipboard from 'vue-clipboard2';
import { Notification } from 'element-ui';
import Fingerprint2 from 'fingerprintjs2';
import VueClipboards from 'vue-clipboards';
import Contextmenu from "vue-contextmenujs"


// 生成唯一ID
Fingerprint2.get(function(components) {
  const values = components.map(function(component,index) {
    if (index === 0) { //把微信浏览器里UA的wifi或4G等网络替换成空,不然切换网络会ID不一样
      return component.value.replace(/\bNetType\/\w+\b/, '');
    }
    return component.value;
  })
  //console.log(values)  //使用的浏览器信息npm
  // 生成最终id
  let port = window.location.port;
  console.log(port);
  const fingerPrint = Fingerprint2.x64hash128(values.join(port), 31)
  Vue.prototype.$browserId = fingerPrint;
  console.log("唯一标识码：" + fingerPrint);
});

Vue.use(VueClipboard);
Vue.use(ElementUI);
Vue.use(VueCookies);
Vue.use(VueClipboards);
Vue.prototype.$axios = axios;
Vue.prototype.$notify = Notification;
Vue.use(Contextmenu);

axios.defaults.baseURL = (process.env.NODE_ENV === 'development') ? process.env.BASE_API : "";

// api 返回401自动回登陆页面
axios.interceptors.response.use(function (response) {
  // 对响应数据做点什么
  return response;
}, function (error) {
  // 对响应错误做点什么
  if (error.response.status === 401) {
    console.log("Received 401 Response")
    router.push('/login');
  }
  return Promise.reject(error);
});

Vue.prototype.$cookies.config(60*30);

new Vue({
	router: router,
	render: h => h(App),
}).$mount('#app')
