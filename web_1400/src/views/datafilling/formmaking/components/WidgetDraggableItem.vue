<template>
  <div>
    <!-- 子表单设计模块 start -->
    <template v-if="element.type === 'subform'">
      <div
        class="subform-box"
        :class="{ active: element.key === selectWidget.key }"
        @click.stop="handleSelectWidget(index)"
      >
        <div class="subform-label">子表单</div>
        <draggable
          v-model="element.list"
          tag="div"
          class="draggable-box"
          v-bind="{ group: moveAllowed ? 'form-draggable' : '', ghostClass: 'moving', animation: 180, handle: '.drag-widget' }"
          @add="handleWidgetAdd"
        >
          <transition-group name="list" tag="div" class="draggable-list">
            <widget-draggable-item
              v-for="(item, i) in element.list"
              :key="item.key"
              class="draggable-move"
              :index="i"
              :element="item"
              :select.sync="selectWidget"
              :data="element"
            />
          </transition-group>
        </draggable>
        <div v-if="selectWidget.key === element.key" class="widget-view-action widget-col-action">
          <i class="el-icon-copy-document" @click.stop="handleWidgetCopy(index)" />
          <i class="el-icon-delete" @click.stop="handleWidgetDelete(index)" />
        </div>
        <div v-if="selectWidget.key === element.key" class="widget-view-drag widget-col-drag">
          <i class="el-icon-rank drag-widget" />
        </div>
      </div>
    </template>
    <!-- 子表单设计模块 end -->
    <!-- 标签Tabs布局 start -->
    <template v-else-if="element.type === 'tabs'">
      <div
        class="tabs-box"
        :class="{ active: element.key === selectWidget.key }"
        @click.stop="handleSelectWidget(index)"
      >
        <el-tabs
          value="1"
          :type="element.options.type"
          :tab-position="element.options.tabPosition"
        >
          <el-tab-pane
            v-for="(tabItem, idnex) in element.tabs"
            :key="idnex"
            :label="tabItem.label"
            :name="tabItem.name"
          >
            <draggable
              v-model="tabItem.list"
              tag="div"
              class="draggable-box"
              v-bind="{ group: 'form-draggable', ghostClass: 'moving', animation: 180, handle: '.drag-widget' }"
              @add="handleWidgetAdd"
            >
              <transition-group name="list" tag="div" class="draggable-list">
                <widget-draggable-item
                  v-for="(item, i) in tabItem.list"
                  :key="item.key"
                  class="draggable-move"
                  :index="i"
                  :element="item"
                  :select.sync="selectWidget"
                  :data="tabItem"
                />
              </transition-group>
            </draggable>
          </el-tab-pane>
        </el-tabs>
        <div v-if="selectWidget.key === element.key" class="widget-view-action widget-col-action">
          <i class="el-icon-copy-document" @click.stop="handleWidgetCopy(index)" />
          <i class="el-icon-delete" @click.stop="handleWidgetDelete(index)" />
        </div>
        <div v-if="selectWidget.key === element.key" class="widget-view-drag widget-col-drag">
          <i class="el-icon-rank drag-widget" />
        </div>
      </div>
    </template>
    <!-- 标签Tabs布局 end -->
    <!-- 栅格布局 start -->
    <template v-else-if="element.type === 'grid'">
      <div
        class="grid-box"
        :class="{ active: element.key === selectWidget.key }"
        @click.stop="handleSelectWidget(index)"
      >
        <el-row :gutter="element.options.gutter">
          <el-col
            v-for="(colItem, idnex) in element.columns"
            :key="idnex"
            :span="colItem.span || 0"
          >
            <draggable
              v-model="colItem.list"
              tag="div"
              class="draggable-box"
              v-bind="{ group: 'form-draggable', ghostClass: 'moving', animation: 180, handle: '.drag-widget' }"
              @add="handleWidgetAdd"
            >
              <transition-group name="list" tag="div" class="draggable-list">
                <widget-draggable-item
                  v-for="(item, i) in colItem.list"
                  :key="item.key"
                  class="draggable-move"
                  :index="i"
                  :element="item"
                  :select.sync="selectWidget"
                  :data="colItem"
                />
              </transition-group>
            </draggable>
          </el-col>
        </el-row>
        <div v-if="selectWidget.key === element.key" class="widget-view-action widget-col-action">
          <i class="el-icon-copy-document" @click.stop="handleWidgetCopy(index)" />
          <i class="el-icon-delete" @click.stop="handleWidgetDelete(index)" />
        </div>
        <div v-if="selectWidget.key === element.key" class="widget-view-drag widget-col-drag">
          <i class="el-icon-rank drag-widget" />
        </div>
      </div>
    </template>
    <!-- 栅格布局 end -->
    <!-- 卡片布局 start -->
    <template v-else-if="element.type === 'card'">
      <div
        class="card-box"
        :class="{ active: element.key === selectWidget.key }"
        @click.stop="handleSelectWidget(index)"
      >
        <el-card class="box-card">
          <div slot="header" class="clearfix">
            <span>{{ element.label }}</span>
          </div>
          <draggable
            v-model="element.list"
            tag="div"
            class="draggable-box"
            v-bind="{ group: 'form-draggable', ghostClass: 'moving', animation: 180, handle: '.drag-widget' }"
            @add="handleWidgetAdd"
          >
            <transition-group name="list" tag="div" class="draggable-list">
              <widget-draggable-item
                v-for="(item, i) in element.list"
                :key="item.key"
                class="draggable-move"
                :index="i"
                :element="item"
                :select.sync="selectWidget"
                :data="element"
              />
            </transition-group>
          </draggable>
        </el-card>
        <div v-if="selectWidget.key === element.key" class="widget-view-action widget-col-action">
          <i class="el-icon-copy-document" @click.stop="handleWidgetCopy(index)" />
          <i class="el-icon-delete" @click.stop="handleWidgetDelete(index)" />
        </div>
        <div v-if="selectWidget.key === element.key" class="widget-view-drag widget-col-drag">
          <i class="el-icon-rank drag-widget" />
        </div>
      </div>
    </template>
    <!-- 卡片布局 end -->
    <!-- 表格布局 start -->
    <template v-else-if="element.type === 'table'">
      <div
        class="table-box"
        :class="{ active: element.key === selectWidget.key }"
        @click.stop="handleSelectWidget(index)"
      >
        <table
          class="table-layout"
          :class="{
            bright: element.options.bright,
            small: element.options.small,
            bordered: element.options.bordered
          }"
          :style="element.options.customStyle"
        >
          <tr v-for="(trItem, trIndex) in element.trs" :key="trIndex">
            <td
              v-for="(tdItem, tdIndex) in trItem.tds"
              :key="tdIndex"
              :colspan="tdItem.colspan"
              :rowspan="tdItem.rowspan"
            >
              <draggable
                v-model="tdItem.list"
                tag="div"
                class="draggable-box"
                v-bind="{ group: 'form-draggable', ghostClass: 'moving', animation: 180, handle: '.drag-widget' }"
                @add="handleWidgetAdd"
              >
                <transition-group name="list" tag="div" class="draggable-list">
                  <widget-draggable-item
                    v-for="(item, i) in tdItem.list"
                    :key="item.key"
                    class="draggable-move"
                    :index="i"
                    :element="item"
                    :select.sync="selectWidget"
                    :data="tdItem"
                  />
                </transition-group>
              </draggable>
            </td>
          </tr>
        </table>
        <div v-if="selectWidget.key === element.key" class="widget-view-action widget-col-action">
          <i class="el-icon-copy-document" @click.stop="handleWidgetCopy(index)" />
          <i class="el-icon-delete" @click.stop="handleWidgetDelete(index)" />
        </div>
        <div v-if="selectWidget.key === element.key" class="widget-view-drag widget-col-drag">
          <i class="el-icon-rank drag-widget" />
        </div>
      </div>
    </template>
    <!-- 表格布局 end -->
    <template v-else>
      <widget-form-item
        :key="element.key"
        :index="index"
        :element="element"
        :select.sync="selectWidget"
        :data="data"
      />
    </template>
  </div>
</template>

<script>
import draggable from 'vuedraggable'
import WidgetFormItem from './WidgetFormItem'

export default {
  name: 'WidgetDraggableItem',
  components: {
    draggable,
    WidgetFormItem
  },
  props: {
    element: {
      type: Object,
      required: true,
      default: () => ({})
    },
    select: {
      type: Object,
      required: true,
      default: () => ({})
    },
    index: {
      type: Number,
      required: true
    },
    data: {
      type: Object,
      required: true,
      default: () => ({})
    }
  },
  data() {
    return {
      moveAllowedType: [
        'input',
        'textarea',
        'number',
        'select',
        'checkbox',
        'radio',
        'date',
        'time',
        'rate',
        'slider',
        'uploadFile',
        'uploadImg',
        'cascader',
        'treeSelect',
        'switch',
        'text',
        'html'
      ],
      selectWidget: this.select
    }
  },
  computed: {
    moveAllowed() {
      return this.moveAllowedType.includes(this.selectWidget.type)
    }
  },
  watch: {
    select(val) {
      this.selectWidget = val
    },
    selectWidget: {
      handler(val) {
        this.$emit('update:select', val)
      },
      deep: true
    }
  },
  methods: {
    handleSelectWidget(index) {
      this.selectWidget = this.data.list[index]
    },
    handleWidgetAdd(evt) {
      // 为拖拽到容器的组件push到list
      const newIndex = evt.newIndex
      const key = new Date().getTime()
      this.$set(this.data.list, newIndex, {
        ...this.data.list[newIndex],
        key,
        model: this.data.list[newIndex].type + '_' + key
      })
      this.selectWidget = this.data.list[newIndex]
    },
    handleWidgetDelete(index) {
      if (this.data.list.length - 1 === index) {
        if (index === 0) {
          this.selectWidget = {}
        } else {
          this.selectWidget = this.data.list[index - 1]
        }
      } else {
        this.selectWidget = this.data.list[index + 1]
      }
      this.$nextTick(() => {
        this.data.list.splice(index, 1)
      })
    },
    handleWidgetCopy(index) {
      const key = new Date().getTime()
      const cloneData = {
        ...this.data.list[index],
        key,
        model: this.data.list[index].type + '_' + key
      }
      // card布局处理
      if (typeof cloneData.list !== 'undefined') {
        cloneData.list = []
      }
      // grid布局处理
      if (typeof cloneData.columns !== 'undefined') {
        cloneData.columns = JSON.parse(JSON.stringify(cloneData.columns))
        // 复制时，清空数据
        cloneData.columns.forEach(item => {
          item.list = []
        })
      }
      // table布局处理
      if (typeof cloneData.trs !== 'undefined') {
        cloneData.trs = JSON.parse(JSON.stringify(cloneData.trs))
        // 复制时，清空数据
        cloneData.trs.forEach(item => {
          item.tds.forEach(val => {
            val.list = []
          })
        })
      }
      // tabs布局处理
      if (typeof cloneData.tabs !== 'undefined') {
        cloneData.tabs = JSON.parse(JSON.stringify(cloneData.tabs))
        // 复制时，清空数据
        cloneData.tabs.forEach(item => {
          item.list = []
        })
      }
      this.data.list.splice(index, 0, cloneData)
      this.$nextTick(() => {
        this.selectWidget = this.data.list[index + 1]
      })
    }
  }
}
</script>

<style scoped>

</style>
