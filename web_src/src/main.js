import Vue from 'vue';
import App from './App.vue';
Vue.config.productionTip = false;
import ElementUI from 'element-ui';
import 'element-ui/lib/theme-chalk/index.css';
import router from './router/index.js';
import axios from 'axios';
import VueCookies from 'vue-cookies';

import echarts from 'echarts';
import VueClipboard from 'vue-clipboard2'
Vue.use(VueClipboard)
Vue.use(ElementUI);
Vue.use(VueCookies);
Vue.prototype.$axios = axios;

axios.defaults.baseURL = (process.env.NODE_ENV === 'development') ? process.env.BASE_API : "";

Vue.prototype.$cookies.config(60*30);


new Vue({
	router: router,
	render: h => h(App),
}).$mount('#app')
