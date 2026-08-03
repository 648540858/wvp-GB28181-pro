<template>
  <a-config-provider
    :locale="zhCN"
    :theme="themeConfig"
    :form="formConfig"
    component-size="middle"
  >
    <div class="app-root">
      <router-view />
    </div>
  </a-config-provider>
</template>

<script>
import { formConfig, locale, themeConfig } from '@/components/ui/config'

export default {
  name: 'App',
  data() {
    return {
      zhCN: locale,
      themeConfig,
      formConfig
    }
  }
}
</script>

<style>
.app-root {
  height: 100%;
}
</style>
