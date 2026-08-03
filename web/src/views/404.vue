<template>
  <main class="not-found-page">
    <div class="not-found-visual" aria-hidden="true">
      <img class="not-found-image" src="@/assets/404_images/404.png" alt="">
    </div>
    <section class="not-found-content">
      <p class="error-code">404</p>
      <h1>页面未找到</h1>
      <p>访问地址不存在、已被移除，或当前账号暂无访问权限。</p>
      <router-link to="/">
        <el-button type="primary" icon="el-icon-s-home">返回控制台</el-button>
      </router-link>
    </section>
  </main>
</template>

<script>
export default {
  name: 'Page404'
}
</script>

<style lang="scss" scoped>
.not-found-page {
  display: grid;
  grid-template-columns: minmax(280px, 520px) minmax(280px, 420px);
  align-items: center;
  justify-content: center;
  gap: 64px;
  min-height: 100dvh;
  padding: 48px 24px;
  color: var(--wvp-text-primary);
  background: var(--wvp-page-background);
}

.not-found-visual {
  min-width: 0;
}

.not-found-image {
  display: block;
  width: 100%;
  height: auto;
}

.not-found-content {
  min-width: 0;
}

.error-code {
  margin: 0 0 8px;
  color: var(--wvp-primary);
  font-size: 48px;
  font-weight: 700;
  line-height: 1;
}

.not-found-content h1 {
  margin: 0 0 16px;
  font-size: 28px;
  font-weight: 600;
  letter-spacing: 0;
}

.not-found-content p:not(.error-code) {
  max-width: 380px;
  margin: 0 0 24px;
  color: var(--wvp-text-secondary);
  font-size: 15px;
  line-height: 1.75;
}

.not-found-content :deep(.ant-btn) {
  min-width: 120px;
}

@media (max-width: 768px) {
  .not-found-page {
    grid-template-columns: minmax(0, 1fr);
    gap: 24px;
    padding: 32px 20px;
    text-align: center;
  }

  .not-found-visual {
    max-width: 420px;
    margin: 0 auto;
  }

  .not-found-content p:not(.error-code) {
    margin-right: auto;
    margin-left: auto;
  }
}
</style>
