<template>
  <div>
    <!-- <div :index="item.key" v-for="(item,i) in  list" :key="i+'-'">
      <el-submenu v-if="item.hasChildren">
          <template slot="title">
            <i class="el-icon-video-camera"></i>
            <span slot="title">{{item.title || item.deviceId}}</span>
          </template>
          <channel-list :list="item.children" @sendDevicePush="sendDevicePush"></channel-list>
      </el-submenu>
      <el-menu-item v-else :index="item.key" @click="sendDevicePush(item)">
        <template slot="title" >
          <i class="el-icon-switch-button" :style="{color:item.status==1?'#67C23A':'#F56C6C'}"></i>
          <span slot="title">{{item.title}}</span>
        </template>
      </el-menu-item>
    </div> -->
    <div >
      <template v-if="!item.hasChildren">
          <el-menu-item :index="item.key" @click="sendDevicePush(item)">
            <i class="el-icon-video-camera" :style="{color:item.status==1?'#67C23A':'#F56C6C'}"></i>
            {{item.title}}
          </el-menu-item>
      </template>

      <el-submenu v-else :index="item.key">
        <template slot="title" >
          <i class="el-icon-location-outline"></i>
          {{item.title}}
        </template>

        <template v-for="child in item.children">
          <channel-item
            v-if="child.hasChildren"
            :item="child"
            :key="child.key"
            @sendDevicePush="sendDevicePush"/>
          <el-menu-item v-else :key="child.key" :index="child.key" @click="sendDevicePush(child)">
            <i class="el-icon-video-camera" :style="{color:child.status==1?'#67C23A':'#F56C6C'}"></i>
            {{child.title}}
          </el-menu-item>
        </template>
      </el-submenu>
    </div>
  </div>
</template>
<script>
export default {
  name:'ChannelItem',
  props:{
    list:Array,
    channelId: String,
    item: {
      type: Object,
      required: true
    }
  },
  data () {
    return {

    }
  },
  watch: {
    channelId(val) {
      console.log(val);
    }
  },
  methods: {
    sendDevicePush(c) {
      this.$emit('sendDevicePush',c)
    }
  }
}
</script>
