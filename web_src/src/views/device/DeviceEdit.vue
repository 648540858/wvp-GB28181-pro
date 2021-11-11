<template>
  <a-modal
    title="设备信息编辑"
    :width="640"
    :visible="visible"
    :confirmLoading="loading"
    @ok="() => { $emit('ok') }"
    @cancel="() => { $emit('cancel') }"
  >
    <a-spin :spinning="loading">
      <a-form :form="form" v-bind="formLayout">
        <a-form-item label="设备编号">
          <a-input v-decorator="['deviceId']" disabled/>
        </a-form-item>
        <a-form-item label="设备名称">
          <a-input v-decorator="['name',{rules:[{required: true, min:2, max:255,message:'只能输入2-255个字符'}]}]"/>
        </a-form-item>
        <a-form-item label="字符集">
          <a-select v-decorator="['charset',{rules: [{required:true, message: '请选择字符集'}]}]"
                    style="width: 100%">
            <a-select-option value="gb2312">GB2312</a-select-option>
            <a-select-option value="utf-8">UTF-8</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-spin>
  </a-modal>
</template>

<script>
import pick from 'lodash.pick'

// 表单字段
const fields = ['deviceId', 'name', 'charset']

export default {
  props: {
    visible: {
      type: Boolean,
      required: true
    },
    loading: {
      type: Boolean,
      default: () => false
    },
    model: {
      type: Object,
      default: () => null
    }
  },
  data() {
    this.formLayout = {
      labelCol: {
        xs: {span: 24},
        sm: {span: 7}
      },
      wrapperCol: {
        xs: {span: 24},
        sm: {span: 13}
      }
    }
    return {
      form: this.$form.createForm(this)
    }
  },
  created() {
    // 防止表单未注册
    fields.forEach(v => this.form.getFieldDecorator(v))

    // 当 model 发生改变时，为表单设置值
    this.$watch('model', () => {
      this.model && this.form.setFieldsValue(pick(this.model, fields))
    })
  }
}
</script>