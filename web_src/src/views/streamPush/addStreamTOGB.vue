<template>
  <a-modal
    title="添加代理"
    :width="900"
    :visible.sync="showDialog"
    :confirmLoading="loading"
    :destroy-on-close="true"
    @ok="handleOk"
    @cancel="cancelForm"
  >
    <a-spin :spinning="loading">
      <a-form-model ref="form" :model="proxyParam" :layout="form.layout" :rules="rules" v-bind="formItemLayout">
        <a-form-model-item label="名称" prop="name">
          <a-input v-model="proxyParam.name" clearable></a-input>
        </a-form-model-item>
        <a-form-model-item label="流应用名" prop="app">
          <a-input v-model="proxyParam.app" clearable :disabled="true"></a-input>
        </a-form-model-item>
        <a-form-model-item label="流ID" prop="stream">
          <a-input v-model="proxyParam.stream" clearable :disabled="true"></a-input>
        </a-form-model-item>
        <a-form-model-item label="国标编码" prop="gbId">
          <a-input v-model="proxyParam.gbId" placeholder="设置国标编码可推送到国标" clearable></a-input>
        </a-form-model-item>
      </a-form-model>
    </a-spin>
  </a-modal>
</template>

<script>

import { addTOGBPush} from "@/api/streamProxy";

export default {
  props: {},
  data() {
    return {
      showDialog: false,
      loading: false,
      proxyParam: {
        name: null,
        app: null,
        stream: null,
        gbId: null,
      },
      form: {
        layout: 'horizontal'
      },
      type: 'default',
      rules: {
        name: [{ required: true, message: "请输入名称", trigger: "blur" }],
        app: [{ required: true, message: "请输入应用名", trigger: "blur" }],
        stream: [{ required: true, message: "请输入流ID", trigger: "blur" }],
        gbId: [{ required: true, message: "请输入国标编码", trigger: "blur" }],
      },
    }
  },
  computed: {
    formItemLayout() {
      const {layout} = this.form;
      return layout === 'horizontal'
        ? {
          labelCol: {
            xs: {span: 24},
            sm: {span: 7}
          },
          wrapperCol: {
            xs: {span: 24},
            sm: {span: 13}
          }
        }
        : {};
    },
  },
  methods: {
    open(proxyParam) {
      this.showDialog = true;
      if (proxyParam != null) {
        this.proxyParam = proxyParam;
      }
    },
    cancelForm() {
      console.log("关闭加入GB");
      this.showDialog = false;
      const form = this.$refs.form
      form.resetFields()
    },
    handleOk() {
      this.confirmLoading = true
      console.log("form",this.$refs.form)
      this.$refs.form.validate(valid => {
        if (!valid) {
          this.confirmLoading = false
          return
        }
        addTOGBPush(this.proxyParam).then(res => {
          console.log(res)
          this.confirmLoading = false
          if (res.code === 200) {
            this.$message.success(res.message)
            this.$emit('refreshTable')
            this.cancelForm()
          }
        }).catch(err => {
          console.log(err)
        })
      })
    },
  },
  mounted() {

  }
}
</script>