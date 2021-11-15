<template>
  <pro-layout
    :menus="menus"
    :collapsed="collapsed"
    :mediaQuery="query"
    :isMobile="isMobile"
    :handleMediaQuery="handleMediaQuery"
    :handleCollapse="handleCollapse"
    :i18nRender="i18nRender"
    v-bind="settings"
  >
    <!-- 1.0.0+ 版本 pro-layout 提供 API，
          我们推荐使用这种方式进行 LOGO 和 title 自定义
    -->
    <template v-slot:menuHeaderRender>
      <div>
        <logo-svg/>
        <h1>{{ title }}</h1>
      </div>
    </template>
    <!-- 1.0.0+ 版本 pro-layout 提供 API,
          增加 Header 左侧内容区自定义
    -->
    <template v-slot:headerContentRender>
      <div style="display: inline-flex; align-items: center">
        <a-switch :default-checked="alarmNotify" checked-children="开" un-checked-children="关" @change="sseControl"/>&nbsp;&nbsp;报警推送开关
      </div>
    </template>

    <setting-drawer v-if="isDev" :settings="settings" @change="handleSettingChange">
      <div style="margin: 12px 0;">
        This is SettingDrawer custom footer content.
      </div>
    </setting-drawer>
    <template v-slot:rightContentRender>
      <right-content/>
    </template>
    <!-- custom footer / 自定义Footer -->
    <template v-slot:footerRender>
      <global-footer/>
    </template>
    <router-view/>
  </pro-layout>
</template>

<script>
import {SettingDrawer, updateTheme} from '@ant-design-vue/pro-layout'
import {i18nRender} from '@/locales'
import {mapState} from 'vuex'
import {CONTENT_WIDTH_TYPE, SIDEBAR_TYPE, TOGGLE_MOBILE_TYPE} from '@/store/mutation-types'

import defaultSettings from '@/config/defaultSettings'
import RightContent from '@/components/GlobalHeader/RightContent'
import GlobalFooter from '@/components/GlobalFooter'
import LogoSvg from '../assets/logo.svg?inline'

export default {
  name: 'BasicLayout',
  components: {
    SettingDrawer,
    RightContent,
    GlobalFooter,
    LogoSvg
  },
  data() {
    return {
      //报警推送相关参数
      alarmNotify: true,
      sseSource: null,
      // end
      // process.env.NODE_ENV === 'development' || process.env.VUE_APP_PREVIEW === 'true'
      isDev: false,

      // base
      menus: [],
      // 侧栏收起状态
      collapsed: false,
      title: defaultSettings.title,
      settings: {
        // 布局类型
        layout: defaultSettings.layout, // 'sidemenu', 'topmenu'
        // CONTENT_WIDTH_TYPE
        contentWidth: defaultSettings.layout === 'sidemenu' ? CONTENT_WIDTH_TYPE.Fluid : defaultSettings.contentWidth,
        // 主题 'dark' | 'light'
        theme: defaultSettings.navTheme,
        // 主色调
        primaryColor: defaultSettings.primaryColor,
        fixedHeader: defaultSettings.fixedHeader,
        fixSiderbar: defaultSettings.fixSiderbar,
        colorWeak: defaultSettings.colorWeak,

        hideHintAlert: false,
        hideCopyButton: false
      },
      // 媒体查询
      query: {},

      // 是否手机模式
      isMobile: false,
    }
  },
  computed: {
    ...mapState({
      // 动态主路由
      mainMenu: state => state.permission.addRouters
    })
  },
  created() {
    const routes = this.mainMenu.find(item => item.path === '/')
    this.menus = (routes && routes.children) || []
    // 处理侧栏收起状态
    this.$watch('collapsed', () => {
      this.$store.commit(SIDEBAR_TYPE, this.collapsed)
    })
    this.$watch('isMobile', () => {
      this.$store.commit(TOGGLE_MOBILE_TYPE, this.isMobile)
    })
  },
  mounted() {
    const userAgent = navigator.userAgent
    if (userAgent.indexOf('Edge') > -1) {
      this.$nextTick(() => {
        this.collapsed = !this.collapsed
        setTimeout(() => {
          this.collapsed = !this.collapsed
        }, 16)
      })
    }

    // first update color
    // TIPS: THEME COLOR HANDLER!! PLEASE CHECK THAT!!
    if (process.env.NODE_ENV !== 'production' || process.env.VUE_APP_PREVIEW === 'true') {
      updateTheme(this.settings.primaryColor)
    }

    window.addEventListener('beforeunload', e => this.sseSource.close())
    this.sseControl(this.alarmNotify)
  },
  methods: {
    i18nRender,
    handleMediaQuery(val) {
      this.query = val
      if (this.isMobile && !val['screen-xs']) {
        this.isMobile = false
        return
      }
      if (!this.isMobile && val['screen-xs']) {
        this.isMobile = true
        this.collapsed = false
        this.settings.contentWidth = CONTENT_WIDTH_TYPE.Fluid
        // this.settings.fixSiderbar = false
      }
    },
    handleCollapse(val) {
      this.collapsed = val
    },
    handleSettingChange({type, value}) {
      console.log('type', type, value)
      type && (this.settings[type] = value)
      switch (type) {
        case 'contentWidth':
          this.settings[type] = value
          break
        case 'layout':
          if (value === 'sidemenu') {
            this.settings.contentWidth = CONTENT_WIDTH_TYPE.Fluid
          } else {
            this.settings.fixSiderbar = false
            this.settings.contentWidth = CONTENT_WIDTH_TYPE.Fixed
          }
          break
      }
    },
    sseClose(){
      this.sseSource.close();
    }
    ,
    sseControl(checked) {
      this.alarmNotify = checked
      if (checked) {
        console.log("申请SSE推送API调用，浏览器ID: " + this.$browserId);
        this.sseSource = new EventSource(process.env.VUE_APP_API_BASE_URL+'/api/emit?browserId=' + this.$browserId);
        let self = this;
        this.sseSource.addEventListener('message', evt => {
          self.$notification.warn({
            message: '报警信息',
            duration: 10,
            description: this.initNotifyContent(evt.data)
          });
        });
        this.sseSource.addEventListener('open', e => {
          console.log("SSE连接打开.");
        }, false);
        this.sseSource.addEventListener('error', e => {
          if (e.target.readyState === EventSource.CLOSED) {
            console.log("SSE连接关闭");
          } else {
            console.log(e.target.readyState);
          }
        }, false);
      } else {
        this.sseSource.removeEventListener('open', null);
        this.sseSource.removeEventListener('message', null);
        this.sseSource.removeEventListener('error', null);
        this.sseSource.close();
      }
    },
    initNotifyContent(htmlStr){
      const h = this.$createElement;
      return h("div", {domProps: {innerHTML: htmlStr}},  null)
    }
  },
  destroyed() {
    window.removeEventListener('beforeunload', e => this.sseSource.close())
    this.sseSource.removeEventListener('open', null);
    this.sseSource.removeEventListener('message', null);
    this.sseSource.removeEventListener('error', null);
    this.sseSource.close();
  }
}
</script>

<style lang="less">
@import "./BasicLayout.less";
</style>
