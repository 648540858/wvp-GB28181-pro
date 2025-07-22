<template>
  <div id="app" style="height: calc(100vh - 124px);">
    <el-form :inline="true" size="mini">
      <el-form-item label="搜索">
        <el-input
          v-model="searchStr"
          style="margin-right: 1rem; width: auto;"
          placeholder="关键字"
          prefix-icon="el-icon-search"
          clearable
          @input="initData"
        />
      </el-form-item>
      <el-form-item label="在线状态">
        <el-select
          v-model="online"
          style="width: 8rem; margin-right: 1rem;"
          placeholder="请选择"
          default-first-option
          @change="initData"
        >
          <el-option label="全部" value="" />
          <el-option label="在线" value="true" />
          <el-option label="离线" value="false" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button icon="el-icon-plus" size="mini" style="margin-right: 1rem;" type="primary" @click="add">新设备</el-button>
        <el-button icon="el-icon-info" style="margin-right: 1rem;" @click="showInfo()">接入信息</el-button>
      </el-form-item>
      <el-form-item style="float: right;">
        <el-button
          icon="el-icon-refresh-right"
          circle
          :loading="getListLoading"
          @click="getList()"
        />
      </el-form-item>
    </el-form>
    <!--设备列表-->
    <el-table
      size="small"
      :data="deviceList"
      height="calc(100% - 64px)"
      header-row-class-name="table-header"
    >
      <el-table-column prop="phoneNumber" label="终端手机号" min-width="120" />
      <el-table-column prop="terminalId" label="终端ID" min-width="120" />
      <el-table-column prop="provinceText" label="省域" min-width="120" />
      <el-table-column prop="cityText" label="市县域" min-width="120" />
      <el-table-column prop="makerId" label="制造商" min-width="120" />
      <el-table-column prop="model" label="型号" min-width="120" />
      <el-table-column label="车牌颜色" min-width="120">
        <template slot-scope="scope">
          <div slot="reference" class="name-wrapper">
            <span v-if="scope.row.plateColor === 1">蓝色</span>
            <span v-else-if="scope.row.plateColor === 2">黄色</span>
            <span v-else-if="scope.row.plateColor === 3">黑色</span>
            <span v-else-if="scope.row.plateColor === 4">白色</span>
            <span v-else-if="scope.row.plateColor === 5">绿色</span>
            <span v-else-if="scope.row.plateColor === 91">农黄色</span>
            <span v-else-if="scope.row.plateColor === 92">农绿色</span>
            <span v-else-if="scope.row.plateColor === 93">黄绿色</span>
            <span v-else-if="scope.row.plateColor === 94">渐变绿</span>
            <span v-else>未上牌</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="plateNo" label="车牌" min-width="120" />
      <el-table-column prop="registerTime" label="注册时间" min-width="160" />
      <el-table-column label="状态" min-width="120">
        <template slot-scope="scope">
          <div slot="reference" class="name-wrapper">
            <el-tag v-if="scope.row.status" size="medium">在线</el-tag>
            <el-tag v-if="!scope.row.status" size="medium" type="info">离线</el-tag>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="操作" min-width="340" fixed="right">
        <template slot-scope="scope">
          <el-divider direction="vertical" />
          <el-button
            type="text"
            size="medium"
            icon="el-icon-video-camera"
            @click="showChannelList(scope.row)"
          >通道
          </el-button>
          <el-divider direction="vertical" />
          <el-button size="medium" icon="el-icon-edit" type="text" @click="edit(scope.row)">编辑</el-button>
          <el-divider direction="vertical" />
          <el-button
            size="medium"
            icon="el-icon-delete"
            type="text"
            style="color: #f56c6c"
            @click="deleteDevice(scope.row)"
          >删除
          </el-button>
          <el-divider direction="vertical" />
          <el-dropdown @command="(command)=>{moreClick(command, scope.row)}">
            <el-button size="medium" type="text">
              更多功能<i class="el-icon-arrow-down el-icon--right" />
            </el-button>
            <el-dropdown-menu slot="dropdown">
              <el-dropdown-item command="params" :disabled="!scope.row.status">
                终端参数</el-dropdown-item>
              <el-dropdown-item command="attribute" v-bind:disabled="!scope.row.status">
                终端属性</el-dropdown-item>
              <el-dropdown-item command="mediaAttribute" v-bind:disabled="!scope.row.status">
                音视频属性</el-dropdown-item>
              <el-dropdown-item command="linkDetection" v-bind:disabled="!scope.row.status" >
                链路检测</el-dropdown-item>
              <el-dropdown-item command="position" v-bind:disabled="!scope.row.status" >
                位置信息</el-dropdown-item>
              <el-dropdown-item command="textMsg" v-bind:disabled="!scope.row.status" >
                文本下发</el-dropdown-item>
              <el-dropdown-item command="telephoneCallback" v-bind:disabled="!scope.row.status" >
                电话回拨</el-dropdown-item>
<!--              <el-dropdown-item command="setPhoneBook" v-bind:disabled="!scope.row.status" >-->
<!--                设置电话本</el-dropdown-item>-->
              <el-dropdown-item command="driverInfo" v-bind:disabled="!scope.row.status" >
                驾驶员信息</el-dropdown-item>
              <el-dropdown-item command="reset" v-bind:disabled="!scope.row.status" >
                终端复位</el-dropdown-item>
              <el-dropdown-item command="factoryReset" v-bind:disabled="!scope.row.status" >
                恢复出厂</el-dropdown-item>
              <el-dropdown-item command="connection" v-bind:disabled="!scope.row.status" >
                连接指定服务器</el-dropdown-item>
              <el-dropdown-item command="door" v-bind:disabled="!scope.row.status" >
                车门控制</el-dropdown-item>
            </el-dropdown-menu>
          </el-dropdown>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      style="float: right"
      :current-page="currentPage"
      :page-size="count"
      :page-sizes="[15, 25, 35, 50]"
      layout="total, sizes, prev, pager, next"
      :total="total"
      @size-change="handleSizeChange"
      @current-change="currentChange"
    />
    <deviceEdit ref="deviceEdit" />
    <configInfo ref="configInfo" />
    <attribute ref="attribute" />
    <position ref="position" />
    <textMsg ref="textMsg" />
    <telephoneCallback ref="telephoneCallback" />
    <driverInfo ref="driverInfo" />
    <connectionServer ref="connectionServer" />
    <controlDoor ref="controlDoor" />
    <mediaAttribute ref="mediaAttribute" />
  </div>
</template>

<script>
import deviceEdit from './edit.vue'
import configInfo from '../dialog/configInfo.vue'
import attribute from './dialog/attribute.vue'
import position from './dialog/position.vue'
import textMsg from './dialog/textMsg.vue'
import telephoneCallback from './dialog/telephoneCallback.vue'
import driverInfo from './dialog/driverInfo.vue'
import connectionServer from './dialog/connectionServer.vue'
import controlDoor from './dialog/controlDoor.vue'
import mediaAttribute from './dialog/mediaAttribute.vue'

export default {
  name: 'App',
  components: {
    deviceEdit, configInfo, attribute, position, textMsg, telephoneCallback, driverInfo, connectionServer, controlDoor, mediaAttribute
  },
  data() {
    return {
      deviceList: [], // 设备列表
      updateLooper: 0, // 数据刷新轮训标志
      winHeight: window.innerHeight - 200,
      searchStr: '',
      online: '',
      currentPage: 1,
      count: 15,
      total: 0,
      getListLoading: false
    }
  },
  mounted() {
    this.initData()
    this.updateLooper = setInterval(this.initData, 10000)
  },
  destroyed() {
    this.$destroy('videojs')
    clearTimeout(this.updateLooper)
  },
  methods: {
    initData: function() {
      this.getList()
    },
    currentChange: function(val) {
      this.currentPage = val
      this.getList()
    },
    handleSizeChange: function(val) {
      this.count = val
      this.getList()
    },
    getList: function() {
      this.getListLoading = true
      this.$store.dispatch('jtDevice/queryDevices', {
        page: this.currentPage,
        count: this.count,
        query: this.searchStr,
        online: this.online
      })
        .then(data => {
          this.total = data.total
          this.deviceList = data.list
        })
        .finally(() => {
          this.getListLoading = false
        })
    },
    deleteDevice: function(row) {
      this.$confirm('确定删除此设备？', '提示', {
        dangerouslyUseHTMLString: true,
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        center: true,
        type: 'warning'
      }).then(() => {
        this.$store.dispatch('jtDevice/deleteDevice', row.phoneNumber)
          .then(data => {
            this.getList()
          })
      }).catch(() => {

      })
    },
    edit: function(row) {
      this.$refs.deviceEdit.openDialog(row, () => {
        this.$refs.deviceEdit.close()
        this.$message({
          showClose: true,
          message: '设备修改成功，通道字符集将在下次更新生效',
          type: 'success'
        })
        setTimeout(this.getList, 200)
      })
    },
    showChannelList: function(row) {
      console.log(row)
      this.$emit('show-channel', row.id)
    },
    showParam: function(row) {
      this.$emit('show-param', row.phoneNumber)
    },
    add: function() {
      this.$refs.deviceEdit.openDialog(null, () => {
        this.$refs.deviceEdit.close()
        this.$message({
          showClose: true,
          message: '添加成功',
          type: 'success'
        })
        setTimeout(this.getList, 200)
      })
    },
    moreClick: function(command, itemData) {
      if (command === 'params') {
        this.showParam(itemData)
      } else if (command === 'attribute') {
         this.queryAttribute(itemData)
      } else if (command === 'linkDetection') {
         this.linkDetection(itemData)
      } else if (command === 'position') {
         this.queryPosition(itemData)
      } else if (command === 'textMsg') {
         this.sendTextMsg(itemData)
      } else if (command === 'telephoneCallback') {
         this.telephoneCallback(itemData)
      } else if (command === 'driverInfo') {
         this.queryDriverInfo(itemData)
      } else if (command === 'factoryReset') {
         this.factoryReset(itemData)
      } else if (command === 'reset') {
         this.reset(itemData)
      } else if (command === 'door') {
         this.controlDoor(itemData)
      } else if (command === 'connection') {
         this.connection(itemData)
      } else if (command === 'mediaAttribute') {
         this.queryMediaAttribute(itemData)
      } else {
        this.$message.info('尚不支持')
      }
    },
    showInfo: function() {
      this.$store.dispatch('server/getSystemConfig')
        .then((data) => {
          this.serverId = data.addOn.serverId
          this.$refs.configInfo.openDialog(data, 'jt1078Config')
        })
    },
    queryAttribute: function(itemData) {
      this.$store.dispatch('jtDevice/queryAttribute', itemData.phoneNumber)
        .then((data) => {
          this.$refs.attribute.openDialog(data)
        })
    },
    queryPosition: function(itemData) {
      this.$store.dispatch('jtDevice/queryPosition', itemData.phoneNumber)
        .then((data) => {
          this.$refs.position.openDialog(data)
        })
    },
    sendTextMsg: function(itemData) {
      this.$refs.textMsg.openDialog(itemData)
    },
    telephoneCallback: function(itemData) {
      this.$refs.telephoneCallback.openDialog(itemData)
    },
    queryDriverInfo: function(itemData) {
      this.$store.dispatch('jtDevice/queryDriverInfo', itemData.phoneNumber)
          .then(data => {
            this.$refs.driverInfo.openDialog(data)
          })
    },
    factoryReset: function(itemData) {
      this.$confirm('确定恢复出厂设置', '提示', {
        dangerouslyUseHTMLString: true,
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.$store.dispatch('jtDevice/factoryReset', itemData.phoneNumber)
            .then(data => {
              this.$message.success({
                showClose: true,
                message: '消息已经下发'
              })
            })
      }).catch(() => {

      })
    },
    reset: function(itemData) {
      this.$confirm('确定开始终端复位', '提示', {
        dangerouslyUseHTMLString: true,
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.$store.dispatch('jtDevice/reset', itemData.phoneNumber)
            .then(data => {
              this.$message.success({
                showClose: true,
                message: '消息已经下发'
              })
            })
      }).catch(() => {

      })
    },
    connection: function(itemData) {
      this.$refs.connectionServer.openDialog(itemData.phoneNumber)
    },
    queryMediaAttribute: function(itemData) {
      this.$store.dispatch('jtDevice/queryMediaAttribute', itemData.phoneNumber)
        .then((data) => {
          this.$refs.mediaAttribute.openDialog(data)
        })

    },
    controlDoor: function(itemData) {
      this.$refs.controlDoor.openDialog(itemData.phoneNumber)
    },
    linkDetection: function(itemData) {
      this.$store.dispatch('jtDevice/linkDetection', itemData.phoneNumber)
        .then((data) => {
          if (data === 0) {
            this.$message.success({
              showClose: true,
              message: '成功'
            })
          }else if (data === 1) {
            this.$message.error({
              showClose: true,
              message: '失败'
            })
          }else if (data === 2) {
            this.$message.error({
              showClose: true,
              message: '消息有误'
            })
          }else if (data === 3) {
            this.$message.error({
              showClose: true,
              message: '不支持此消息'
            })
          }
        })
    }
  }
}
</script>
