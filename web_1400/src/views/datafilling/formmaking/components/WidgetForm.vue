<template>
  <div class="widget-form-wrapper" :style="{ width: data.config.width + '%' }">
    <el-form :size="data.config.size" label-suffix=":" :label-position="data.config.labelPosition" :label-width="data.config.labelWidth + 'px'">
      <div v-if="data.list.length === 0" class="form-empty">从左侧拖拽或点击来添加字段</div>
      <draggable
        v-model="data.list"
        tag="div"
        class="draggable-box"
        v-bind="{ group: 'form-draggable', ghostClass: 'moving', animation: 180, handle: '.drag-widget' }"
        @add="handleWidgetAdd"
      >
        <transition-group name="list" tag="div" class="draggable-list">
          <widget-draggable-item
            v-for="(element, index) in data.list"
            :key="element.key"
            class="draggable-move"
            :index="index"
            :element="element"
            :select.sync="selectWidget"
            :data="data"
          />
        </transition-group>
      </draggable>
    </el-form>
  </div>
</template>

<script>
import draggable from 'vuedraggable'
import WidgetDraggableItem from './WidgetDraggableItem'

export default {
  name: 'WidgetForm',
  components: {
    draggable,
    WidgetDraggableItem
  },
  props: {
    data: {
      type: Object,
      required: true,
      default: () => ({})
    },
    select: {
      type: Object,
      default: () => ({})
    }
  },
  data() {
    return {
      selectWidget: this.select
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
      // 删除icon属性
      delete this.data.list[newIndex].icon
      this.selectWidget = this.data.list[newIndex]
    }
  }
}
</script>

<style lang="scss" scoped>

</style>
