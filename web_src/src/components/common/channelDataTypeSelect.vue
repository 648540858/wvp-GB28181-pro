<template>
  <div id="channelDataTypeSelect" >
    <el-select size="mini"  @change="$emit('change', $event.target.value)" v-model="dataType" placeholder="请选择"
               default-first-option>
      <el-option label="全部" value=""></el-option>
      <el-option
        v-for="item in dataTypeArray"
        :key="item.key"
        :label="item.key"
        :value="item.value">
      </el-option>
    </el-select>
  </div>
</template>

<script>

export default {
  name: "channelDataTypeSelect",
  model: {
    prop: 'dataType',
    event: 'change'
  },
  props: {
    dataType: {
      type: String,
      default: ''
    }
  },
  components: {},
  created() {
    this.init()
  },
  data() {
    return {
      dataTypeArray: []
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
<style>
.channel-form {
  display: grid;
  background-color: #FFFFFF;
  padding: 1rem 2rem 0 2rem;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 1rem;
}

</style>
