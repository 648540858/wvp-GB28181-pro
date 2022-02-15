<template>
  <div>
      <el-tree :data="channelList" :props="props" @node-click="sendDevicePush">
        <span  slot-scope="{ node }">
          <span v-if="node.isLeaf">
            <i class="el-icon-video-camera" :style="{color:node.disabled==1?'#67C23A':'#F56C6C'}"></i>
          </span>
          <span v-else>
            <i class="el-icon-folder"></i>
          </span>
          <span>
            {{ node.label }}
          </span>
        </span>
      </el-tree>
  </div>
</template>
<script>
import ChannelTreeItem from "@/components/channelTreeItem" 
import {tree} from '@/api/deviceApi'

export default {
  components: {
    ChannelTreeItem,
  },
  props:{
    device: {
      type: Object,
      required: true
    }
  },
  data() {
      return {
        loading: false,
        channelList: [],
        props: {
          label: 'title',
          children: 'children',
          isLeaf: 'hasChildren',
          disabled: 'status'
        },
      }
  },
  computed: {
     
  },
  mounted() {
    this.leafs = []
    this.getTree()
  },
  methods: {
    getTree() {
      this.loading = true
      var that = this
      tree(this.device.deviceId).then(function (res) {
          console.log(res.data.data);
          that.channelList = res.data.data;
          that.loading = false;
        }).catch(function (error) {
          console.log(error);
          that.loading = false;
        });
    },
    sendDevicePush(c) {
      if(c.hasChildren) return
      this.$emit('sendDevicePush',c)
    }
  }
}
</script>