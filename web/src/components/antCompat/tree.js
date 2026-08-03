import { defineComponent, h, onMounted, ref, watch } from 'vue'
import { Tree } from 'ant-design-vue'

export default defineComponent({
  name: 'VueEasyTree',
  inheritAttrs: false,
  props: {
    data: { type: Array, default: () => [] },
    nodeKey: { type: String, default: 'id' },
    props: { type: Object, default: () => ({ label: 'label', children: 'children' }) },
    load: Function,
    lazy: Boolean,
    height: [String, Number],
    defaultExpandedKeys: { type: Array, default: () => [] }
  },
  emits: ['node-click', 'node-contextmenu'],
  setup(props, { attrs, slots, emit, expose }) {
    const roots = ref([])
    const expandedKeys = ref([...props.defaultExpandedKeys])
    const nodeMap = new Map()
    const labelField = () => props.props.label || 'label'
    const childrenField = () => props.props.children || 'children'

    const rootNode = {
      key: '__root__',
      level: 0,
      data: {},
      parent: null,
      loaded: false,
      childNodes: [],
      expand: () => loadChildren(rootNode)
    }

    function dataKey(data, index) {
      return String(data?.[props.nodeKey] ?? data?.id ?? data?.deviceId ?? index)
    }

    function createItem(data, parent = rootNode, level = 1, index = 0) {
      const key = dataKey(data, index)
      const node = {
        key,
        level,
        data,
        parent,
        loaded: Array.isArray(data?.[childrenField()]),
        childNodes: [],
        expand: async() => {
          if (!node.loaded) await loadChildren(node)
          if (!expandedKeys.value.includes(key)) expandedKeys.value = [...expandedKeys.value, key]
        },
        collapse: () => {
          expandedKeys.value = expandedKeys.value.filter(item => item !== key)
        }
      }
      Object.defineProperty(node, 'label', { get: () => data?.[labelField()] })
      const item = {
        key,
        title: data?.[labelField()],
        isLeaf: Boolean(data?.isLeaf ?? data?.leaf),
        raw: data,
        compatNode: node
      }
      const children = data?.[childrenField()]
      if (Array.isArray(children)) {
        item.children = children.map((child, childIndex) => createItem(child, node, level + 1, childIndex))
        node.childNodes = item.children.map(child => child.compatNode)
      }
      node.item = item
      nodeMap.set(key, node)
      return item
    }

    function replaceChildren(node, data) {
      const parent = node || rootNode
      const items = data.map((item, index) => createItem(item, parent, parent.level + 1, index))
      parent.loaded = true
      parent.childNodes = items.map(item => item.compatNode)
      if (parent === rootNode) {
        roots.value = items
      } else {
        parent.item.children = items
        roots.value = [...roots.value]
      }
    }

    function loadChildren(node) {
      if (!props.load) return Promise.resolve()
      return new Promise((resolve, reject) => {
        try {
          props.load(node, data => {
            replaceChildren(node, Array.isArray(data) ? data : [])
            resolve()
          })
        } catch (error) {
          reject(error)
        }
      })
    }

    function remove(data, parentNode) {
      const key = dataKey(data)
      const parent = parentNode || rootNode
      if (parent === rootNode) {
        roots.value = roots.value.filter(item => item.key !== key)
      } else {
        parent.item.children = (parent.item.children || []).filter(item => item.key !== key)
        parent.childNodes = parent.item.children.map(item => item.compatNode)
        roots.value = [...roots.value]
      }
      nodeMap.delete(key)
    }

    function append(data, parentNode) {
      const parent = parentNode || rootNode
      const item = createItem(data, parent, parent.level + 1, parent.childNodes.length)
      if (parent === rootNode) {
        roots.value = [...roots.value, item]
      } else {
        parent.item.children = [...(parent.item.children || []), item]
        parent.childNodes = parent.item.children.map(child => child.compatNode)
        roots.value = [...roots.value]
      }
    }

    expose({
      append,
      remove,
      getNode: key => nodeMap.get(String(key))
    })

    watch(() => props.data, data => {
      if (data.length) replaceChildren(rootNode, data)
    }, { deep: true })

    onMounted(() => {
      if (props.data.length) replaceChildren(rootNode, props.data)
      else if (props.lazy) loadChildren(rootNode)
    })

    return () => h(Tree, {
      ...attrs,
      treeData: roots.value,
      expandedKeys: expandedKeys.value,
      height: typeof props.height === 'number' ? props.height : undefined,
      blockNode: true,
      class: ['el-tree', attrs.class],
      loadData: treeNode => {
        const node = treeNode.compatNode
        return node && !node.loaded ? loadChildren(node) : Promise.resolve()
      },
      'onUpdate:expandedKeys': keys => { expandedKeys.value = keys },
      onClick: (event, treeNode) => {
        const node = treeNode.compatNode
        emit('node-click', node.data, node, treeNode)
      },
      onRightClick: ({ event, node: treeNode }) => {
        const node = treeNode.compatNode
        emit('node-contextmenu', event, node.data, node, treeNode)
      }
    }, {
      title: treeNode => slots.default
        ? slots.default({ node: treeNode.compatNode, data: treeNode.raw })
        : treeNode.title
    })
  }
})
