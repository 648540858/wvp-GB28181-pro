<template>
  <section class="app-main">
    <div class="content">
      <transition name="fade-transform" mode="out-in">
        <router-view :key="key" />
      </transition>
    </div>
  </section>
</template>

<script>
export default {
  name: 'AppMain',
  computed: {
    key() {
      return this.$route.path
    }
  }
}
</script>

<style lang="scss" scoped>
@import "~@/styles/variables.scss";

.app-main {
  /*107 = navbar 50 + topbar 57  */
  height: calc(100vh - 107px);
  width: 100%;
  position: relative;
  overflow-x: hidden;
  overflow-y: auto;
  .content {
    padding: 6px;
    min-height: calc(100vh - 39px - 107px);
  }
}
.fixed-header+.app-main {
  padding-top: 50px;
}
</style>

<style lang="scss">
// fix css style bug in open el-dialog
.el-popup-parent--hidden {
  .fixed-header {
    padding-right: 15px;
  }
}
</style>
