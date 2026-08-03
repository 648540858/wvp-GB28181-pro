<template>
  <main id="main-content" class="app-main" tabindex="-1">
    <router-view />
  </main>
</template>

<script>
export default {
  name: 'AppMain'
}
</script>

<style scoped>
.app-main {
  min-height: calc(100vh - var(--wvp-shell-height));
  min-height: calc(100dvh - var(--wvp-shell-height));
  width: 100%;
  position: relative;
  overflow-x: hidden;
  background: var(--wvp-page-background);
  outline: none;
}

.app-main > :deep(*) {
  animation: wvp-page-enter 0.2s ease-out both;
}
.fixed-header+.app-main {
  padding-top: var(--wvp-shell-height);
}

@keyframes wvp-page-enter {
  from {
    opacity: 0;
    transform: translateY(4px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
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
