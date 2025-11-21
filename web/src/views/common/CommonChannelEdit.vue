<template>
  <div id="CommonChannelEdit" v-loading="loading" style="width: 100%; height: calc(-218px + 100vh); overflow: auto;">
    <el-form ref="channelForm" :model="form" :rules="rules" status-icon label-width="160px" class="channel-form" size="medium">
      <div class="form-box">
        <el-form-item label="名称" prop="gbName">
          <el-input v-model="form.gbName" placeholder="请输入通道名称" />
        </el-form-item>
        <el-form-item label="编码" prop="gbDeviceId">
          <el-input v-model="form.gbDeviceId" placeholder="请输入通道编码">
            <template v-slot:append>
              <el-button @click="buildDeviceIdCode(form.gbDeviceId)">生成</el-button>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="设备厂商">
          <el-input v-model="form.gbManufacturer" placeholder="请输入设备厂商" />
        </el-form-item>
        <el-form-item label="设备型号">
          <el-autocomplete
            style="width: 100%;"
            v-model="form.gbModel"
            value-key="name"
            :fetch-suggestions="queryModel"
            placeholder="请输入内容"
          >
            <template slot-scope="{ item }">
              <span class="addr">{{ item.name }}（{{ item.alias }}）</span>
            </template>
          </el-autocomplete>
        </el-form-item>

        <el-form-item label="行政区域">
          <el-input v-model="form.gbCivilCode" placeholder="请输入行政区域" @change="getRegionPaths">
            <template v-slot:append>
              <el-button @click="chooseCivilCode()">选择</el-button>
            </template>
          </el-input>
          <el-breadcrumb v-if="regionPath.length > 0" separator="/" style="display: block; margin-top: 8px; font-size: 14px;">
            <el-breadcrumb-item v-for="key in regionPath" :key="key">{{ key }}</el-breadcrumb-item>
          </el-breadcrumb>
        </el-form-item>

        <el-form-item label="安装地址">
          <el-input v-model="form.gbAddress" placeholder="请输入安装地址" />
        </el-form-item>
        <el-form-item label="监视方位">
          <el-select v-model="form.gbDirectionType" style="width: 100%" placeholder="请选择监视方位">
            <el-option label="东(西向东)" :value="1" />
            <el-option label="西(东向西)" :value="2" />
            <el-option label="南(北向南)" :value="3" />
            <el-option label="北(南向北)" :value="4" />
            <el-option label="东南(西北到东南)" :value="5" />
            <el-option label="东北(西南到东北)" :value="6" />
            <el-option label="西南(东北到西南)" :value="7" />
            <el-option label="西北(东南到西北)" :value="8" />
            <el-option label="左(非标)" :value="91" />
            <el-option label="后(非标)" :value="92" />
            <el-option label="前(非标)" :value="93" />
            <el-option label="右(非标)" :value="94" />
            <el-option label="左前(非标)" :value="95" />
            <el-option label="右前(非标)" :value="96" />
            <el-option label="左后(非标)" :value="97" />
            <el-option label="右后(非标)" :value="98" />
          </el-select>
        </el-form-item>

        <el-form-item label="父节点编码">
          <el-input v-model="form.gbParentId" placeholder="请输入父节点编码或选择所属虚拟组织" @change="getPaths">
            <template v-slot:append>
              <el-button @click="chooseGroup()">选择</el-button>
            </template>
          </el-input>
          <el-breadcrumb v-if="parentPath.length > 0" separator="/" style="display: block; margin-top: 8px; font-size: 14px;">
            <el-breadcrumb-item v-for="key in parentPath" :key="key">{{ key }}</el-breadcrumb-item>
          </el-breadcrumb>
        </el-form-item>
        <el-form-item label="设备状态">
          <el-select v-model="form.gbStatus" style="width: 100%" placeholder="请选择设备状态">
            <el-option label="在线" value="ON" />
            <el-option label="离线" value="OFF" />
          </el-select>
        </el-form-item>
        <el-form-item label="经度">
          <el-input v-model="form.gbLongitude" placeholder="请输入经度" />
        </el-form-item>
        <el-form-item label="纬度">
          <el-input v-model="form.gbLatitude" placeholder="请输入纬度" />
        </el-form-item>
        <el-form-item label="摄像机类型">
          <el-select v-model="form.gbPtzType" style="width: 100%" placeholder="请选择摄像机类型">
            <el-option label="球机" :value="1" />
            <el-option label="半球" :value="2" />
            <el-option label="固定枪机" :value="3" />
            <el-option label="遥控枪机" :value="4" />
            <el-option label="遥控半球" :value="5" />
            <el-option label="多目设备的全景/拼接通道" :value="6" />
            <el-option label="多目设备的分割通道" :value="7" />
            <el-option label="移动设备（非标）" :value="99" />
            <el-option label="会议设备（非标）" :value="98" />
          </el-select>
        </el-form-item>
      </div>
      <div>
        <el-form-item label="业务分组编号">
          <el-input v-model="form.gbBusinessGroupId" placeholder="请输入业务分组编号" @change="getPaths"/>
        </el-form-item>
        <el-form-item label="警区">
          <el-input v-model="form.gbBlock" placeholder="请输入警区" />
        </el-form-item>
        <el-form-item label="信令安全模式">
          <el-select v-model="form.gbSafetyWay" style="width: 100%" placeholder="请选择信令安全模式">
            <el-option label="不采用" :value="0" />
            <el-option label="S/MIME签名" :value="2" />
            <el-option label="S/MIME加密签名同时采用" :value="3" />
            <el-option label="数字摘要" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="注册方式">
          <el-select v-model="form.gbRegisterWay" style="width: 100%" placeholder="请选择注册方式">
            <el-option label="IETFRFC3261标准" :value="1" />
            <el-option label="基于口令的双向认证" :value="2" />
            <el-option label="基于数字证书的双向认证注册" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="证书序列号">
          <el-input v-model="form.gbCertNum" type="number" placeholder="请输入证书序列号" />
        </el-form-item>
        <el-form-item label="证书有效标识">
          <el-select v-model="form.gbCertifiable" style="width: 100%" placeholder="请选择证书有效标识">
            <el-option label="有效" :value="1" />
            <el-option label="无效" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="无效原因码">
          <el-input v-model="form.gbCertNum" type="errCode" placeholder="请输入无效原因码" />
        </el-form-item>
        <el-form-item label="证书终止有效期">
          <el-date-picker
            v-model="form.gbEndTime"
            type="datetime"
            placeholder="选择日期时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="保密属性">
          <el-select v-model="form.gbSecrecy" style="width: 100%" placeholder="请选择保密属性">
            <el-option label="不涉密" :value="0" />
            <el-option label="涉密" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item label="IP地址">
          <el-input v-model="form.gbIpAddress" placeholder="请输入IP地址" />
        </el-form-item>
        <el-form-item label="端口">
          <el-input v-model="form.gbPort" type="number" placeholder="请输入端口" />
        </el-form-item>
        <el-form-item label="设备口令">
          <el-input v-model="form.gbPassword" placeholder="请输入设备口令" />
        </el-form-item>
      </div>
      <div>
        <el-form-item label="设备归属">
          <el-input v-model="form.gbOwner" placeholder="请输入设备归属" />
        </el-form-item>
        <el-form-item label="子设备">
          <el-select v-model="form.gbParental" style="width: 100%" placeholder="请选择是否有子设备">
            <el-option label="有" :value="1" />
            <el-option label="无" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="位置类型">
          <el-select v-model="form.gbPositionType" style="width: 100%" placeholder="请选择位置类型">
            <el-option label="省际检查站" :value="1" />
            <el-option label="党政机关" :value="2" />
            <el-option label="车站码头" :value="3" />
            <el-option label="中心广场" :value="4" />
            <el-option label="体育场馆" :value="5" />
            <el-option label="商业中心" :value="6" />
            <el-option label="宗教场所" :value="7" />
            <el-option label="校园周边" :value="8" />
            <el-option label="治安复杂区域" :value="9" />
            <el-option label="交通干线" :value="10" />
          </el-select>
        </el-form-item>
        <el-form-item label="室外/室内">
          <el-select v-model="form.gbRoomType" style="width: 100%" placeholder="请选择位置类型">
            <el-option label="室外" :value="1" />
            <el-option label="室内" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="用途">
          <el-select v-model="form.gbUseType" style="width: 100%" placeholder="请选择用途类型">
            <el-option label="治安" :value="1" />
            <el-option label="交通" :value="2" />
            <el-option label="重点" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="补光">
          <el-select v-model="form.gbSupplyLightType" style="width: 100%" placeholder="请选择补光类型">
            <el-option label="无补光" :value="1" />
            <el-option label="红外补光" :value="2" />
            <el-option label="白光补光" :value="3" />
            <el-option label="激光补光" :value="4" />
            <el-option label="其他" :value="9" />
          </el-select>
        </el-form-item>
        <el-form-item label="分辨率">
          <el-input v-model="form.gbResolution" placeholder="请输入分辨率" />
        </el-form-item>
        <el-form-item label="下载倍速">
          <el-select v-model="form.gbDownloadSpeedArray" multiple style="width: 100%" placeholder="请选择下载倍速">
            <el-option label="1倍速" value="1" />
            <el-option label="2倍速" value="2" />
            <el-option label="4倍速" value="4" />
            <el-option label="8倍速" value="8" />
            <el-option label="16倍速" value="16" />
          </el-select>
        </el-form-item>
        <el-form-item label="空域编码能力">
          <el-select v-model="form.gbSvcSpaceSupportMod" style="width: 100%" placeholder="请选择空域编码能力">
            <el-option label="1级增强" value="1" />
            <el-option label="2级增强" value="2" />
            <el-option label="3级增强" value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="时域编码能力">
          <el-select v-model="form.gbSvcTimeSupportMode" style="width: 100%" placeholder="请选择时域编码能力">
            <el-option label="1级增强" value="1" />
            <el-option label="2级增强" value="2" />
            <el-option label="3级增强" value="3" />
          </el-select>
        </el-form-item>
        <el-form-item >
          <el-checkbox v-model="form.enableBroadcastForBool" >语音对讲(非标属性)</el-checkbox>
        </el-form-item>
        <div style="text-align: right">
          <el-button type="primary" @click="onSubmit" >保存</el-button>
          <el-button v-if="showCancel" @click="cancelSubmit" >取消</el-button>
          <el-button v-if="form.dataType === 1" @click="showReset">重置</el-button>
        </div>
      </div>

    </el-form>
    <channelCode ref="channelCode" />
    <chooseCivilCode ref="chooseCivilCode" />
    <chooseGroup ref="chooseGroup" />
    <resetChannel ref="resetChannel" @submit="reset"/>
  </div>
</template>

<script>
import channelCode from './../dialog/channelCode'
import ChooseCivilCode from '../dialog/chooseCivilCode.vue'
import ChooseGroup from '../dialog/chooseGroup.vue'
import diff from '../../utils/diff'
import ResetChannel from './../dialog/resetChannel.vue'

export default {
  name: 'CommonChannelEdit',
  components: {
    ResetChannel,
    ChooseCivilCode,
    ChooseGroup,
    channelCode
  },
  props: ['id', 'dataForm', 'showCancel'],
  data() {
    return {
      rules: {
        gbName: [
          { required: true, message: '请输入通道名称', trigger: 'blur' }
        ],
        gbDeviceId: [
          { required: true, message: '请输入通道编号', trigger: 'blur' }
        ]
      },
      loading: false,
      modelList: [],
      parentPath: [],
      regionPath: [],
      form: {}
    }
  },
  mounted() {
    this.$store.dispatch('server/getModelList')
      .then((data) => {
        console.log(data)
        this.modelList = data
      })
  },
  created() {
    // 获取完整信息
    if (this.id) {
      this.getCommonChannel(this.id)
    } else {
      if (!this.dataForm.gbDeviceId) {
        this.dataForm.gbDeviceId = ''
      }
      this.form = window.structuredClone(this.dataForm)
      this.getPaths()
    }
  },
  methods: {
    queryModel(queryString, callback) {
      // 过滤可选项
      let modelList = this.modelList
      var results = queryString ? modelList.filter(((state) => {
        return (state.alias.toLowerCase().indexOf(queryString.toLowerCase()) === 0 || state.name.toLowerCase().indexOf(queryString.toLowerCase()) === 0)
      })) : modelList
      callback(results)
    },
    onSubmit: function() {
      this.$refs.channelForm.validate((valid) => {
        if (valid) {
          this.loading = true
          if (this.form.gbDownloadSpeedArray) {
            this.form.gbDownloadSpeed = this.form.gbDownloadSpeedArray.join('/')
          }
          this.form.enableBroadcast = this.form.enableBroadcastForBool ? 1 : 0
          // 判断哪些字段变化
          let diffData = diff(this.dataForm, this.form)
          diffData['gbId'] = this.form.gbId

          console.log(diffData)
          console.log(this.dataForm)
          console.log(this.form)

          if (this.form.gbId) {
            this.$store.dispatch('commonChanel/update', diffData)
              .then(data => {
                this.$message.success({
                  showClose: true,
                  message: '保存成功'
                })
                this.$emit('submitSuccess')
              })
              .catch((error) => {
                this.$message({
                  showClose: true,
                  message: error,
                  type: 'error'
                })
              })
              .finally(() => {
              this.loading = false
            })
          } else {
            this.$store.dispatch('commonChanel/add', this.form)
              .then(data => {
                this.$message.success({
                  showClose: true,
                  message: '保存成功'
                })
                if (this.saveSuccess) {
                  this.saveSuccess()
                }
              })
              .catch((error) => {
                this.$message({
                  showClose: true,
                  message: error,
                  type: 'error'
                })
              })
              .finally(() => {
              this.loading = false
            })
          }
        }
      })
    },
    reset: function(fileIds) {
      this.$confirm('确定重置为默认内容?', '提示', {
        dangerouslyUseHTMLString: true,
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.loading = true
        this.$store.dispatch('commonChanel/reset', {
          id: this.form.gbId,
          chanelFields: fileIds
        })
          .then((data) => {
            this.$message.success({
              showClose: true,
              message: '重置成功 已保存'
            })
            this.getCommonChannel(this.form.gbId)
          })
          .catch((error) => {
            this.$message({
              showClose: true,
              message: error,
              type: 'error'
            })
          })
          .finally(() => {
            this.loading = false
          })
      }).catch(() => {

      })
    },
    getCommonChannel: function(id) {
      this.loading = true
      this.$store.dispatch('commonChanel/queryOne', id)
        .then(data => {
          if (data.gbDownloadSpeed) {
            data.gbDownloadSpeedArray = data.gbDownloadSpeed.split('/')
          }
          this.dataForm = window.structuredClone(data)
          this.form = data
          this.$set(this.form, 'enableBroadcastForBool', this.form.enableBroadcast === 1)
          this.getPaths()
          this.getRegionPaths()
        })
        .catch((error) => {
          this.$message({
            showClose: true,
            message: error,
            type: 'error'
          })
        })
        .finally(() => {
          this.loading = false
        })
    },
    buildDeviceIdCode: function(deviceId) {
      this.$refs.channelCode.openDialog(code => {
        this.form.gbDeviceId = code
      }, deviceId)
    },
    chooseCivilCode: function() {
      this.$refs.chooseCivilCode.openDialog(code => {
        this.form.gbCivilCode = code
        this.getRegionPaths()
      })
    },
    chooseGroup: function() {
      this.$refs.chooseGroup.openDialog((deviceId, businessGroupId) => {
        this.form.gbBusinessGroupId = businessGroupId
        this.form.gbParentId = deviceId
        this.getPaths()
      })
    },
    cancelSubmit: function() {
      this.$emit('cancel')
    },
    showReset: function() {
      this.$refs.resetChannel.openDialog()
    },
    getPaths: function() {
      this.parentPath = []
      if (this.form.gbParentId && this.form.gbBusinessGroupId) {
        this.$store.dispatch('group/getPath', {
          deviceId: this.form.gbParentId,
          businessGroup: this.form.gbBusinessGroupId
        })
          .then(data => {
            console.log(data)
            const path = []
            for (let i = 0; i < data.length; i++) {
              path.push(data[i].name)
            }
            this.parentPath = path
          })
      }
    },
    getRegionPaths: function() {
      this.regionPath = []
      if (this.form.gbCivilCode) {
        this.$store.dispatch('region/queryPath', this.form.gbCivilCode)
          .then(data => {
            console.log(data)
            const path = []
            for (let i = 0; i < data.length; i++) {
              path.push(data[i].name)
            }
            this.regionPath = path
          })
      }
    }
  }
}
</script>
<style>
.channel-form {
  display: grid;
  background-color: #FFFFFF;
  padding: 1rem 2rem 0 2rem;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 1rem;
}
</style>
