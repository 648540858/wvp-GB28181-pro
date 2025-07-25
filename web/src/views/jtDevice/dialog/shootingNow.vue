<template>
  <div id="configInfo">
    <el-dialog
      v-el-drag-dialog
      title="立即拍摄"
      width="60%"
      top="2rem"
      :close-on-click-modal="false"
      :visible.sync="showDialog"
      :destroy-on-close="true"
      @close="close()"
    >
      <el-form size="small" @submit.native.prevent>
        <el-form-item>
          <el-form inline  @submit.native.prevent>
            <el-form-item style="margin-right: 14.5rem">
              <el-radio-group v-model="commandType">
                <el-radio :label="true" border>拍摄</el-radio>
                <el-radio :label="false" border>录像</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-form>
        </el-form-item>
        <el-form-item>
          <el-form inline size="small" @submit.native.prevent>
            <el-form-item label="录像时长" v-if="!commandType">
              <el-input type="number" v-model="time" placeholder="一直录像" style="width: 8rem"></el-input>
            </el-form-item>
            <el-form-item label="连拍" v-if="commandType">
              <el-input type="number" v-model="commandNumber" placeholder="连拍张数" style="width: 4rem"></el-input>
            </el-form-item>
            <el-form-item label="拍照间隔" v-if="commandType">
              <el-input type="number" v-model="time" placeholder="最小间隔拍照" style="width: 8rem"></el-input>
            </el-form-item>
            <el-form-item label="存储方式">
              <el-select
                v-model="save"
                style="width: 8rem"
                placeholder="请选择存储方式"
              >
                <el-option label="实时上传" :value="0" />
                <el-option label="保存" :value="1" />
              </el-select>
            </el-form-item>
            <el-form-item label="通道">
              <el-select
                v-model="chanelId"
                style="width: 8rem"
                placeholder="请选择通道"
              >
                <el-option v-for="item in channelList" :key="item.id" :label="item.name" :value="item.channelId" />
              </el-select>
            </el-form-item>
            <el-form-item label="分辨率">
              <el-select
                v-model="resolvingPower"
                style="width: 8rem"
                placeholder="请选择分辨率"
              >
                <el-option label="最低分辨率" :value="0x00" />
                <el-option label="320×240" :value="0x01" />
                <el-option label="640×480" :value="0x02" />
                <el-option label="800×600" :value="0x03" />
                <el-option label="1024×768" :value="0x04" />
                <el-option label="176×144" :value="0x05" />
                <el-option label="352×288" :value="0x06" />
                <el-option label="704×288" :value="0x07" />
                <el-option label="704×576" :value="0x08" />
                <el-option label="最高分辨率" :value="0xff" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-checkbox v-model="showImageConfig">高级配置</el-checkbox>
            </el-form-item>
            <el-form-item style="margin-left: 2rem; text-align: right" >
              <el-button type="primary" :loading="queryLoading" icon="el-icon-search" @click="shooting()" >
                执行
              </el-button>
            </el-form-item>
          </el-form>
        </el-form-item>
        <el-form-item v-if="showImageConfig">
          <el-form size="small" label-width="80px" style="display: grid; grid-template-columns: 1fr 1fr; grid-gap: 0.5rem">
            <el-form-item label="质量" prop="topSpeed" >
              <el-slider v-model="quality" show-input :height="1" :marks="qualityMarks" :min="1" :max="10" :step="1"/>
            </el-form-item>
            <el-form-item label="亮度" prop="brightness">
              <el-slider v-model="brightness" show-input :height="1" :min="0" :max="255" :step="1" />
            </el-form-item>
            <el-form-item label="对比度" prop="contrastRatio">
              <el-slider v-model="contrastRatio" show-input :height="1" :min="0" :max="127" :step="1"/>
            </el-form-item>
            <el-form-item label="饱和度" prop="saturation">
              <el-slider v-model="saturation" show-input :height="1" :min="0" :max="127" :step="1"/>
            </el-form-item>
            <el-form-item label="色度" prop="chroma">
              <el-slider v-model="chroma" show-input :height="1" :min="0" :max="255" :step="1"/>
            </el-form-item>
          </el-form>
        </el-form-item>
      </el-form>
      <queryMediaList :phoneNumber="phoneNumber" :deviceId="deviceId" :channelList="channelList"></queryMediaList>
    </el-dialog>
  </div>
</template>

<script>

import elDragDialog from '@/directive/el-drag-dialog'
import queryMediaList from './queryMediaList.vue'

export default {
  name: 'ConfigInfo',
  directives: { elDragDialog },
  components: { queryMediaList },
  props: {},
  data() {
    return {
      deviceId: null,
      phoneNumber: null,
      showDialog: false,
      queryLoading: false,
      showImageConfig: false,
      commandType: true,
      commandNumber: 1,
      time: null,
      save: 1,
      chanelId: null,
      resolvingPower: 0xff,
      qualityMarks: {
        1: '最优',
        10: '最差'
      },
      quality: 1,
      brightness: 125,
      contrastRatio: 63,
      saturation: 63,
      chroma: 125,
      channelList: [],
    }
  },
  computed: {},
  created() {},
  methods: {
    openDialog: function(phoneNumber, deviceId) {
      console.log(phoneNumber)
      this.showDialog = true
      this.phoneNumber = phoneNumber
      this.deviceId = deviceId
      this.$store.dispatch('jtDevice/queryChannels', {
        page: 1,
        count: 1000,
        deviceId: this.deviceId
      })
        .then(data => {
          this.channelList = data.list
          this.chanelId = data.list[0].channelId
        })

    },
    close: function() {
      this.showDialog = false
      this.channelList = []
      this.type = 0
      this.chanelId = null
    },
    shooting: function() {
      this.$store.dispatch('jtDevice/shooting', {
        phoneNumber: this.phoneNumber,
        shootingCommand: {
          chanelId: this.chanelId,
          command: !this.commandType? 0xFFFF : this.commandNumber,
          time: this.time,
          save: this.save,
          resolvingPower: this.resolvingPower,
          quality: this.quality,
          brightness: this.brightness,
          contrastRatio: this.contrastRatio,
          saturation: this.saturation,
          chroma: this.chroma
        }
      })
        .then( data => {
          this.$message.success({
            showClose: true,
            message: '消息已经下发'
          })
        })
    }
  }
}
</script>

<style scoped>
>>> .el-upload {
  width: 100% !important;
}
>>> .el-slider__marks-text {
  margin-top: -36px;
  font-size: 12px;
  width: 2rem !important;
}
</style>
