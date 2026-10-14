/**
 * 系统设置-服务配置面板通用逻辑
 * <p>
 * 提供服务重启后的等待恢复能力, 供各服务配置面板复用
 */
export default {
  data() {
    return {
      restarting: false
    }
  },
  methods: {
    // 提示是否立即重启服务
    confirmRestart(onDone) {
      this.$confirm(
        '服务即将重启，重启期间页面将无法访问，请稍后刷新页面。',
        '重启服务',
        {
          confirmButtonText: '立即重启',
          cancelButtonText: '稍后手动重启',
          type: 'warning'
        }
      ).then(() => {
        this.doRestart(onDone)
      }).catch(() => {
        this.$message.info('可稍后手动重启服务使配置生效')
      })
    },
    doRestart(onDone) {
      this.$store.dispatch('setting/restartServer').then(() => {
        this.$message.success('服务正在重启，请稍后刷新页面')
        this.restarting = true
        this.waitForRestart(onDone)
      }).catch(error => {
        this.$message.error(typeof error === 'string' ? error : '触发重启失败')
      })
    },
    // 重启后轮询等待服务恢复
    waitForRestart(onDone) {
      let retry = 0
      const maxRetry = 60
      const timer = setInterval(() => {
        retry += 1
        this.$store.dispatch('setting/getProfileList')
          .then(() => {
            clearInterval(timer)
            this.restarting = false
            this.$message.success('服务已恢复')
            if (typeof onDone === 'function') {
              onDone()
            }
          })
          .catch(() => {
            if (retry >= maxRetry) {
              clearInterval(timer)
              this.restarting = false
              this.$message.warning('等待服务恢复超时，请手动刷新页面')
            }
          })
      }, 3000)
    }
  }
}
