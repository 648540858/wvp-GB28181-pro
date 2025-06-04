<template>
  <div
    class="draggable-move-box"
    :class="{ active: selectWidget.key === element.key, 'is_hidden':element.options.hidden }"
    @click.stop="handleSelectWidget(index)"
  >
    <div class="form-item-box">
      <el-form-item
        v-if="element && element.key"
        :class="{ 'is_req': element.options.required }"
        :label="element.label"
      >
        <template v-if="element.type === 'input'">
          <el-input
            v-model="element.options.defaultValue"
            :style="{width: element.options.width}"
            :disabled="element.options.disabled"
            :placeholder="element.options.placeholder"
            :type="element.options.type"
            :clearable="element.options.clearable"
            :max-length="element.options.maxLength"
          />
        </template>
        <template v-if="element.type === 'textarea'">
          <el-input
            v-model="element.options.defaultValue"
            type="textarea"
            :rows="4"
            :style="{width: element.options.width}"
            :disabled="element.options.disabled"
            :placeholder="element.options.placeholder"
            :auto-size="{
              minRows: element.options.minRows,
              maxRows: element.options.maxRows
            }"
            :max-length="element.options.maxLength"
          />
        </template>
        <template v-if="element.type === 'number'">
          <el-input-number
            v-model="element.options.defaultValue"
            :style="{width: element.options.width}"
            :disabled="element.options.disabled"
            :controls-position="element.options.controlsPosition"
            :min="element.options.min || element.options.min === 0 ? element.options.min : -Infinity"
            :max="element.options.max || element.options.max === 0 ? element.options.max : Infinity"
            :step="element.options.step"
            :precision="element.options.precision > 50 || (!element.options.precision && element.options.precision !== 0) ? null : element.options.precision"
          />
        </template>
        <template v-if="element.type == 'select'">
          <el-select
            v-model="element.options.defaultValue"
            :disabled="element.options.disabled"
            :multiple="element.options.multiple"
            :clearable="element.options.clearable"
            :placeholder="element.options.placeholder"
            :style="{width: element.options.width}"
          >
            <el-option v-for="(item, index) in element.options.options" :key="item.value + index" :value="item.value" :label="item.label" />
          </el-select>
        </template>
        <template v-if="element.type == 'radio'">
          <el-radio-group
            v-model="element.options.defaultValue"
            :disabled="element.options.disabled"
            :style="{width: element.options.width}"
          >
            <el-radio
              v-for="(item, index) in element.options.options"
              :key="item.value + index"
              :style="{display: element.options.inline ? 'inline-block' : 'block'}"
              :label="item.value"
            >
              {{ item.label }}
            </el-radio>
          </el-radio-group>
        </template>
        <template v-if="element.type == 'checkbox'">
          <el-checkbox-group
            v-model="element.options.defaultValue"
            :disabled="element.options.disabled"
            :style="{width: element.options.width}"
          >
            <el-checkbox
              v-for="(item, index) in element.options.options"
              :key="item.value + index"
              :style="{display: element.options.inline ? 'inline-block' : 'block'}"
              :label="item.value"
            >
              {{ item.label }}
            </el-checkbox>
          </el-checkbox-group>
        </template>
        <template v-if="element.type == 'date'">
          <el-date-picker
            v-model="element.options.defaultValue"
            :type="element.options.type"
            :placeholder="element.options.placeholder"
            :start-placeholder="element.options.startPlaceholder"
            :end-placeholder="element.options.endPlaceholder"
            :readonly="element.options.readonly"
            :disabled="element.options.disabled"
            :editable="element.options.editable"
            :clearable="element.options.clearable"
            :style="{width: element.options.width}"
          />
        </template>
        <template v-if="element.type == 'time'">
          <el-time-picker
            v-model="element.options.defaultValue"
            :is-range="element.options.isRange"
            :placeholder="element.options.placeholder"
            :start-placeholder="element.options.startPlaceholder"
            :end-placeholder="element.options.endPlaceholder"
            :readonly="element.options.readonly"
            :disabled="element.options.disabled"
            :editable="element.options.editable"
            :clearable="element.options.clearable"
            :arrow-control="element.options.arrowControl"
            :style="{width: element.options.width}"
          />
        </template>
        <template v-if="element.type === 'rate'">
          <el-rate
            v-model="element.options.defaultValue"
            :max="element.options.max"
            :disabled="element.options.disabled"
            :allow-half="element.options.allowHalf"
            :show-score="element.options.showScore"
            :style="{width: element.options.width}"
          />
        </template>
        <template v-if="element.type === 'slider'">
          <el-slider
            v-model="element.options.defaultValue"
            :min="element.options.min"
            :max="element.options.max"
            :disabled="element.options.disabled"
            :step="element.options.step"
            :show-input="element.options.showInput"
            :style="{width: element.options.width}"
          />
        </template>
        <template v-if="element.type == 'color'">
          <el-color-picker
            v-model="element.options.defaultValue"
            :disabled="element.options.disabled"
            :show-alpha="element.options.showAlpha"
          />
        </template>
        <template v-if="element.type === 'switch'">
          <el-switch
            v-model="element.options.defaultValue"
            :disabled="element.options.disabled"
          />
        </template>
        <template v-if="element.type == 'cascader'">
          <el-cascader
            v-model="element.options.defaultValue"
            :disabled="element.options.disabled"
            :clearable="element.options.clearable"
            :placeholder="element.options.placeholder"
            :style="{width: element.options.width}"
            :options="element.options.options"
            :props="element.options.props"
          />
        </template>
        <template v-if="element.type == 'editor'">
          <vue-editor
            v-model="element.options.defaultValue"
            :style="{width: element.options.width}"
          />
        </template>
        <template v-if="element.type === 'alert'">
          <el-alert
            :title="element.options.title"
            :description="element.options.description"
            :type="element.options.type"
            :effect="element.options.effect"
            :show-icon="element.options.showIcon"
            :closable="element.options.closable"
            :center="element.options.center"
          />
        </template>
        <template v-if="element.type === 'text'">
          <div :style="{ textAlign: element.options.textAlign }">
            <label v-text="element.options.defaultValue" />
          </div>
        </template>
        <template v-if="element.type === 'html'">
          <div v-html="element.options.defaultValue" />
        </template>
        <template v-if="element.type === 'divider'">
          <el-divider :content-position="element.options.orientation">{{ element.label }}</el-divider>
        </template>
      </el-form-item>
    </div>
    <div v-if="selectWidget.key === element.key" class="widget-view-action">
      <i class="el-icon-copy-document" @click.stop="handleWidgetCopy(index)" />
      <i class="el-icon-delete" @click.stop="handleWidgetDelete(index)" />
    </div>
    <div v-if="selectWidget.key === element.key" class="widget-view-drag">
      <i class="el-icon-rank drag-widget" />
    </div>
    <div v-if="selectWidget.key === element.key" class="widget-view-model">
      <span>{{ selectWidget.model }}</span>
    </div>
  </div>
</template>

<script>
import { VueEditor } from 'vue2-editor'

export default {
  name: 'WidgetFormItem',
  components: { VueEditor },
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
      this.data.list.splice(index, 0, cloneData)
      this.$nextTick(() => {
        this.selectWidget = this.data.list[index + 1]
      })
    }
  }
}
</script>

<style lang="scss" scoped>

</style>
