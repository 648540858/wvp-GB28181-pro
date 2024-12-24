<template>
  <el-select size="mini"  @change="change" v-model="dataType" placeholder="请选择"
             default-first-option>
    <el-option label="全部" value=""></el-option>
    <el-option
      v-for="item in dataTypeArray"
      :key="item.key"
      :label="item.key"
      :value="item.value">
    </el-option>
  </el-select>
</template>

<script>

export default {
  name: "channelDataTypeSelect",
  props: ['dataType','change'],
  created() {
    this.init()
  },
  data() {
    return {
      dataTypeArray: [],
    };
  },
  methods: {
    init: function (){
      this.$axios({
        method: 'get',
        url: `/api/server/channel/datatype`,
        params: {}
      }).then((res)=> {
        if (res.data.code === 0) {
          this.dataTypeArray = res.data.data;
        }
      })
    },
  },
};
</script>
