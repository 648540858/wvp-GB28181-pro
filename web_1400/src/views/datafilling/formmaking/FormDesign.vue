<template>
  <div class="form-design-container">
    <el-container class="form-design-content">
      <el-aside class="left" width="250px">
        <template v-if="formComponents.length">
          <div class="widget-cate">表单组件</div>
          <draggable
            tag="ul"
            :list="formComponents"
            v-bind="{ group: { name: 'form-draggable', pull: 'clone', put: false }, sort: false, animation: 180, ghostClass: 'moving' }"
          >
            <li v-for="(item, index) in formComponents" :key="index" @click="handleListPush(item)">
              <a>
                <i class="icon el-icon-star-off" />
                <span>{{ item.label }}</span>
              </a>
            </li>
          </draggable>
        </template>
        <template v-if="layoutComponents.length">
          <div class="widget-cate">布局组件</div>
          <draggable
            tag="ul"
            :list="layoutComponents"
            v-bind="{ group: { name: 'form-draggable', pull: 'clone', put: false }, sort: false, animation: 180, ghostClass: 'moving' }"
          >
            <li v-for="(item, index) in layoutComponents" :key="index" @click="handleListPush(item)">
              <a>
                <i class="icon el-icon-star-off" />
                <span>{{ item.label }}</span>
              </a>
            </li>
          </draggable>
        </template>
      </el-aside>
      <el-container class="widget-center-container" direction="vertical">
        <el-header class="btn-bar" style="height: 45px;">
          <el-button type="text" size="medium" icon="el-icon-view">预览</el-button>
          <el-button type="text" size="medium" icon="el-icon-refresh">重置</el-button>
          <el-button type="text" size="medium" icon="el-icon-plus">保存</el-button>
          <el-button type="text" size="medium" icon="el-icon-delete">清空</el-button>
        </el-header>
        <el-main>
          <widget-form :data="widgetData" :select.sync="selectWidget" />
        </el-main>
      </el-container>
      <el-aside class="right">Aside</el-aside>
    </el-container>
  </div>
</template>

<script>
import { formComponents, layoutComponents } from './config/componentsConfig.js'
import draggable from 'vuedraggable'
import WidgetForm from './components/WidgetForm'

export default {
  name: 'FormDesign',
  components: {
    draggable,
    WidgetForm
  },
  data() {
    return {
      formComponents,
      layoutComponents,
      widgetData: {
        list: [],
        config: {
          width: 100,
          size: 'small',
          labelWidth: 100,
          labelPosition: 'right',
          customStyle: ''
        }
      },
      selectWidget: {}
    }
  },
  methods: {
    handleListPush(item) {
      // 双击组件push到list
      const key = new Date().getTime()
      item = {
        ...item,
        key,
        model: item.type + '_' + key
      }
      // json深拷贝一次
      const element = JSON.parse(JSON.stringify(item))
      // 删除icon属性
      delete element.icon
      this.widgetData.list.push(element)
      this.selectWidget = element
    }
  }
}
</script>

<style lang="scss" scoped>
@import '~@/styles/form-design.scss'
</style>
