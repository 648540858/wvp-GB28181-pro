<template>
  <div :class="classObj" class="app-wrapper">
    <a class="skip-link" href="#main-content">跳转到主要内容</a>
    <div v-if="device==='mobile'&&sidebar.opened" class="drawer-bg" @click="handleClickOutside" />
    <sidebar class="sidebar-container" />
    <div class="main-container">
      <div :class="{'fixed-header':fixedHeader}">
        <navbar />
        <tags-view />
      </div>
      <app-main />
    </div>
  </div>
</template>

<script>
import { Navbar, Sidebar, AppMain, TagsView } from './components'
import ResizeMixin from './mixin/ResizeHandler'

export default {
  name: 'Layout',
  components: {
    Navbar,
    Sidebar,
    AppMain,
    TagsView
  },
  mixins: [ResizeMixin],
  computed: {
    sidebar() {
      return this.$store.state.app.sidebar
    },
    device() {
      return this.$store.state.app.device
    },
    fixedHeader() {
      return this.$store.state.settings.fixedHeader
    },
    classObj() {
      return {
        hideSidebar: !this.sidebar.opened,
        openSidebar: this.sidebar.opened,
        withoutAnimation: this.sidebar.withoutAnimation,
        mobile: this.device === 'mobile'
      }
    }
  },
  methods: {
    handleClickOutside() {
      this.$store.dispatch('app/closeSideBar', { withoutAnimation: false })
    }
  }
}
</script>

<style lang="scss" scoped>
  @use "@/styles/mixin" as *;
  @use "@/styles/variables" as *;

  .app-wrapper {
    @include clearfix;
    position: relative;
    height: 100%;
    width: 100%;
    &.mobile.openSidebar{
      position: fixed;
      top: 0;
    }
  }

  .skip-link {
    position: fixed;
    top: 8px;
    left: 8px;
    z-index: 4000;
    padding: 8px 12px;
    color: #fff;
    background: var(--wvp-primary-active);
    border-radius: var(--wvp-radius-md);
    transform: translateY(-160%);
    transition: transform var(--wvp-transition);

    &:focus {
      transform: translateY(0);
    }
  }

  .drawer-bg {
    background: rgba(15, 23, 42, 0.48);
    width: 100%;
    top: 0;
    height: 100%;
    position: absolute;
    z-index: 999;
  }

  .fixed-header {
    position: fixed;
    top: 0;
    right: 0;
    z-index: 1000;
    width: calc(100% - #{$sideBarWidth});
    transition: width var(--wvp-transition);
    background: var(--wvp-surface);
    border-bottom: 1px solid var(--wvp-border-light);
  }

  .hideSidebar .fixed-header {
    width: calc(100% - #{$sideBarCollapsedWidth});
  }

  .mobile .fixed-header {
    width: 100%;
  }
</style>
