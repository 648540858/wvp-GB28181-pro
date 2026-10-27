<template>
  <div id="configInfo">
    <el-dialog
      v-el-drag-dialog
      title="接入信息"
      top="2rem"
      :close-on-click-modal="false"
      :visible.sync="showDialog"
      :destroy-on-close="true"
      @close="close()"
    >
      <div class="config-info-wrap">
        <div class="config-info-left">
          <el-descriptions v-if="isSipSection" title="国标服务信息" :column="2">
            <template slot="extra">
              <el-button
                v-clipboard="copySipJson"
                v-clipboard:success="onCopySuccess"
                v-clipboard:error="onCopyError"
                size="mini"
                icon="el-icon-document-copy"
                title="点击拷贝"
              >复制</el-button>
            </template>
            <el-descriptions-item label="编号">{{ configInfoData.sip.id }}</el-descriptions-item>
            <el-descriptions-item label="域">{{ configInfoData.sip.domain }}</el-descriptions-item>
            <el-descriptions-item label="IP">
              <el-tag
                v-for="(ip, index) in sipIps"
                :key="index"
                size="small"
                style="margin-right: 4px;"
              >{{ ip }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="端口">{{ configInfoData.sip.port }}</el-descriptions-item>
            <el-descriptions-item label="密码">
              <el-tag size="small">{{ configInfoData.sip.password }}</el-tag>
            </el-descriptions-item>
          </el-descriptions>
          <el-descriptions v-if="key === 'jt1078Config' && configInfoData.jt1078Config" title="部标服务信息" :column="2">
            <el-descriptions-item label="端口">{{ configInfoData.jt1078Config.port }}</el-descriptions-item>
            <el-descriptions-item label="密码">
              <el-tag size="small">{{ configInfoData.jt1078Config.password }}</el-tag>
            </el-descriptions-item>
          </el-descriptions>
        </div>
        <div v-if="isSipSection" class="config-info-qr">
          <div class="qr-box">
            <img v-if="qrCodeDataUrl" :src="qrCodeDataUrl" alt="平台接入信息二维码" />
            <span v-else class="qr-loading">二维码生成中...</span>
          </div>
          <div class="qr-tip">可使用国标客户端扫描快速接入</div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script>

import QRCode from 'qrcode'
import elDragDialog from '@/directive/el-drag-dialog'

export default {
  name: 'ConfigInfo',
  directives: { elDragDialog },
  props: {},
  data() {
    return {
      showDialog: false,
      key: null,
      configInfoData: {
        sip: {}

      },
      qrCodeDataUrl: ''
    }
  },
  computed: {
    // 复制/二维码内容：仅包含页面展示字段，IP 字段名统一为 ip（取页面展示值）
    copySipJson() {
      const sip = this.configInfoData.sip || {}
      return JSON.stringify({
        id: sip.id,
        domain: sip.domain,
        ip: sip.showIp,
        port: sip.port,
        password: sip.password
      }, null, 2)
    },
    isSipSection() {
      return (!this.key || this.key === 'sip') && !!this.configInfoData.sip
    },
    // 页面 IP 展示值（showIp）按逗号拆分为列表，便于多网卡场景下逐个区分
    sipIps() {
      const showIp = (this.configInfoData.sip || {}).showIp
      if (!showIp) {
        return []
      }
      return String(showIp).split(',').map(ip => ip.trim()).filter(ip => ip !== '')
    }
  },
  created() {},
  methods: {
    onCopySuccess() {
      this.$message({ type: 'success', message: '平台信息已复制到剪贴板' })
    },
    onCopyError() {
      this.$message({ type: 'error', message: '复制失败，请手动复制' })
    },
    async generateQrCode() {
      this.qrCodeDataUrl = ''
      try {
        // 二维码内容与复制的平台信息保持一致，2 倍尺寸生成保证清晰
        this.qrCodeDataUrl = await QRCode.toDataURL(this.copySipJson, {
          width: 320,
          margin: 2,
          errorCorrectionLevel: 'M'
        })
      } catch (e) {
        console.error('二维码生成失败', e)
        this.$message({ type: 'error', message: '二维码生成失败，请复制信息后使用二维码工具生成' })
      }
    },
    openDialog: function(data, key) {
      this.showDialog = true
      this.key = key
      this.configInfoData = data
      if (this.isSipSection) {
        this.generateQrCode()
      }
    },
    close: function() {
      this.showDialog = false
    }
  }
}
</script>

<style scoped>
.config-info-wrap {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 32px;
  margin-top: 1rem;
}
.config-info-left {
  flex: 1;
  min-width: 0;
}
/* 内容过长时：字段名不压缩换行；
   Element UI 将内容列渲染为 inline-flex（默认 nowrap），多标签会溢出到相邻列，
   这里放开换行，让标签在自身单元格内折行 */
.config-info-left >>> .el-descriptions-item__label {
  flex-shrink: 0;
  white-space: nowrap;
}
.config-info-left >>> .el-descriptions-item__content {
  flex-wrap: wrap;
  row-gap: 6px;
  min-width: 0;
}
.config-info-qr {
  flex: 0 0 204px;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 14px 16px 12px;
  background-color: #fafbfc;
  border: 1px solid #ebeef5;
  border-radius: 8px;
}
.qr-box {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 160px;
  height: 160px;
  overflow: hidden;
  background-color: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
}
.qr-box img {
  display: block;
  width: 100%;
  height: 100%;
}
.qr-loading {
  font-size: 13px;
  color: #909399;
}
.qr-tip {
  margin-top: 12px;
  font-size: 12px;
  color: #909399;
  text-align: center;
}
</style>
