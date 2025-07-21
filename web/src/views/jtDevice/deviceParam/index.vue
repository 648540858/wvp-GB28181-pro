<template>
  <div id="jtDeviceParam" style="width: 100%">
    <div class="page-header">
      <div class="page-title">
        <el-page-header content="终端参数" @back="showDevice" />
      </div>
    </div>
    <el-tabs tab-position="left" v-model="activeTab" style="height: calc(100vh - 164px); padding: 20px">
      <el-tab-pane label="通讯参数" name="communication">
        <communication v-if="activeTab === 'communication'" :phone-number="phoneNumber" @submit="onSubmit" @show-device="showDevice"></communication>
      </el-tab-pane>
      <el-tab-pane label="服务器" name="server">
        <server v-if="activeTab === 'server'" :phone-number="phoneNumber" @submit="onSubmit" @show-device="showDevice"></server>
      </el-tab-pane>
      <el-tab-pane label="位置汇报" name="position">
        <position v-if="activeTab === 'position'" :phone-number="phoneNumber" @submit="onSubmit" @show-device="showDevice"></position>
      </el-tab-pane>
      <el-tab-pane label="电话号码" name="phoneNumber">
        <phoneNumber v-if="activeTab === 'phoneNumber'" :phone-number="phoneNumber" @submit="onSubmit" @show-device="showDevice"></phoneNumber>
      </el-tab-pane>
      <el-tab-pane label="报警参数" name="alarm">
        <alarm v-if="activeTab === 'alarm'" :phone-number="phoneNumber" @submit="onSubmit" @show-device="showDevice"></alarm>
      </el-tab-pane>
      <el-tab-pane label="行驶参数" name="driving">
        <driving v-if="activeTab === 'driving'" :phone-number="phoneNumber" @submit="onSubmit" @show-device="showDevice"></driving>
      </el-tab-pane>
      <el-tab-pane label="定时拍照" name="cameraTimer">
        <cameraTimer v-if="activeTab === 'cameraTimer'" :phone-number="phoneNumber" @submit="onSubmit" @show-device="showDevice"></cameraTimer>
      </el-tab-pane>
      <el-tab-pane label="音视频" name="media">
        <media v-if="activeTab === 'media'" :phone-number="phoneNumber" @submit="onSubmit" @show-device="showDevice"></media>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script>
import communication from './communication.vue'
import server from './server.vue'
import position from './position.vue'
import phoneNumber from './phoneNumber.vue'
import alarm from './alarm.vue'
import driving from './driving.vue'
import cameraTimer from './cameraTimer.vue'
import media from './media.vue'

export default {
  name: 'JTDeviceParam',
  components: {
    communication, server, position, phoneNumber, alarm, driving, cameraTimer, media
  },
  props: {
    phoneNumber: {
      type: String,
      default: null
    }
  },
  data() {
    return {
      form: {
        collisionAlarmParams: {},
        illegalDrivingPeriods: {},
        cameraTimer: {}
      },
      rules: {
        deviceId: [{ required: true, message: '请输入设备编号', trigger: 'blur' }]
      },
      isLoading: false,
      activeTab: 'communication'
    }
  },

  mounted() {
    this.initData()
  },
  methods: {
    initData: function() {
      this.isLoading = true
      this.$store.dispatch('jtDevice/queryConfig', this.phoneNumber)
        .then((data) => {
          this.form = data
        })
        .catch((e) => {
          console.log(e)
        })
        .finally(() => {
          this.isLoading = false
        })
    },
    onSubmit: function(data) {
      this.$store.dispatch('jtDevice/setConfig', {
        phoneNumber: this.phoneNumber,
        config: data
      })
        .then((data) => {
          this.$message.success({
            showClose: true,
            message: '保存成功'
          })
        })
    },
    showDevice: function() {
      this.$emit('show-device')
    }
  }
}
</script>
