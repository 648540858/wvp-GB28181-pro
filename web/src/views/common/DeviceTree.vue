<template>
  <div id="DeviceTree" class="device-tree-container" style="height: 100%">
    <div class="device-tree-header">
      <div class="header-title">通道列表</div>
      <div class="header-switch">
        <el-switch
          v-model="showRegion"
          active-color="#13ce66"
          inactive-color="rgb(64, 158, 255)"
          active-text="行政区划"
          inactive-text="业务分组"
        />
      </div>
    </div>
    <div class="tree-content">
      <div class="tree-wrapper">
        <RegionTree
          v-if="showRegion"
          ref="regionTree"
          :edit="false"
          :show-header="false"
          :has-channel="true"
          @clickEvent="treeNodeClickEvent"
          :default-expanded-keys="[]"
        />
        <GroupTree
          v-if="!showRegion"
          ref="groupTree"
          :edit="false"
          :show-header="false"
          :has-channel="true"
          @clickEvent="treeNodeClickEvent"
          :default-expanded-keys="[]"
        />
      </div>
    </div>
  </div>
</template>

<script>
import RegionTree from './RegionTree.vue'
import GroupTree from './GroupTree.vue'

export default {
  name: 'DeviceTree',
  components: { GroupTree, RegionTree },
  props: {
    device: {
      type: Object,
      default: () => ({})
    },
    onlyCatalog: {
      type: Boolean,
      default: false
    },
    contextMenuEvent: {
      type: Function,
      default: null
    }
  },
  data() {
    return {
      showRegion: true,
      defaultProps: {
        children: 'children',
        label: 'name',
        isLeaf: 'isLeaf'
      }
    }
  },
  mounted() {
    // Apply fix for Element UI tree scrollbars after component is mounted
    this.$nextTick(() => {
      this.fixTreeScrollbars()
      this.adjustTreeHeight()

      // Add resize event listener to handle window resizing
      window.addEventListener('resize', this.adjustTreeHeight)
    })
  },
  updated() {
    // Re-apply fix when component updates (e.g., when switching between RegionTree and GroupTree)
    this.$nextTick(() => {
      this.fixTreeScrollbars()
      this.adjustTreeHeight()
    })
  },
  beforeDestroy() {
    // Remove event listener when component is destroyed
    window.removeEventListener('resize', this.adjustTreeHeight)
  },
  methods: {
    adjustTreeHeight() {
      // Get the container height
      const containerHeight = this.$el.clientHeight

      // Get the header height
      const headerHeight = this.$el.querySelector('.device-tree-header').clientHeight

      // Calculate available height for tree
      const availableHeight = containerHeight - headerHeight - 30 // 30px for padding

      // Set the tree content height
      const treeContent = this.$el.querySelector('.tree-content')
      if (treeContent) {
        treeContent.style.height = `${availableHeight}px`
      }

      // Ensure tree components adapt to the available height
      const treeComponents = this.$el.querySelectorAll('.el-tree')
      treeComponents.forEach(tree => {
        tree.style.height = '100%'
        tree.style.maxHeight = '100%'
      })
    },
    fixTreeScrollbars() {
      // Find all el-tree elements within this component and fix their scrolling behavior
      const trees = this.$el.querySelectorAll('.el-tree')
      trees.forEach(tree => {
        tree.style.overflow = 'visible'
        tree.style.width = '100%'

        // Also fix any scrollable containers within the tree
        const scrollContainers = tree.querySelectorAll('[style*="overflow"]')
        scrollContainers.forEach(container => {
          if (container.style.overflow === 'auto' || container.style.overflow === 'scroll') {
            container.style.overflow = 'visible'
          }
        })
      })
    },
    handleClick: function(tab, event) {
    },
    treeNodeClickEvent: function(data) {
      if (data.leaf) {
        this.$emit('clickEvent', data.id)
      }
    }
  }
}
</script>

<style>
.device-tree-container {
  width: 100%;
  height: 100%;
  background-color: #FFFFFF;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
  padding: 15px;
  overflow: hidden !important; /* Force no overflow on container */
}

.device-tree-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
  flex-wrap: wrap;
  gap: 10px;
  min-height: 30px;
}

.header-title {
  font-size: 16px;
  font-weight: 500;
}

.tree-content {
  flex: 1;
  overflow: hidden !important;
  width: 100%;
  margin: 0;
  padding: 0;
  position: relative;
}

.tree-wrapper {
  width: 100%;
  height: 100%;
  min-width: 0; /* Prevent flex items from overflowing */
  position: relative;
}

/* Global fixes for Element UI tree components */
.el-tree {
  overflow: visible !important;
  width: 100% !important;
  min-width: 0 !important;
  height: 100% !important;
}

.el-tree-node {
  width: 100% !important;
  min-width: 0 !important;
}

.el-tree-node__content {
  width: 100% !important;
  min-width: 0 !important;
}

.el-tree-node__label {
  word-break: break-word !important;
  white-space: normal !important;
}

/* Fix for any scrollable containers */
[style*="overflow: auto"],
[style*="overflow:auto"],
[style*="overflow: scroll"],
[style*="overflow:scroll"] {
  overflow: visible !important;
}

/* Make sure tree nodes are fully visible */
.el-tree-node__children {
  overflow: visible !important;
}

/* Ensure tree nodes can be expanded/collapsed */
.el-tree-node__expand-icon {
  cursor: pointer;
}

.device-tree-main-box {
  text-align: left;
}

.device-online {
  color: #252525;
}

.device-offline {
  color: #727272;
}

/* Responsive adjustments */
@media (max-width: 768px) {
  .device-tree-container {
    padding: 10px;
  }

  .device-tree-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .header-switch {
    width: 100%;
    margin-top: 5px;
  }
}

@media (max-width: 480px) {
  .device-tree-container {
    padding: 8px;
  }

  .header-title {
    font-size: 14px;
  }

  /* Adjust el-switch text size for mobile */
  .el-switch__label {
    font-size: 12px;
  }
}
</style>
