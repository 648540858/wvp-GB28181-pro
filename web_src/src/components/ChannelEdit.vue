<template>
  <div id="ChannelEdit" v-loading="isLoging" style="width: 100%">
    <el-form ref="passwordForm" :rules="rules" status-icon label-width="80px">
<!--      <el-form-item label="gbName" prop="gbName">-->
<!--        <el-input v-model="form.gbName"></el-input>-->
<!--      </el-form-item>-->

      <el-form-item>
        <div style="float: right;">
          <el-button type="primary" @click="onSubmit">保存</el-button>
          <el-button @click="close">取消</el-button>
        </div>
      </el-form-item>
    </el-form>
  </div>
</template>

<script>

export default {
  name: "channelEdit",
  props: [ 'id',],
  computed: {},
  created() {

  },
  data() {
    return {
      form: {},
    };
  },
  methods: {
    onSubmit: function () {

    },
    close: function () {

    },
    getAllRole:function () {

      this.$axios({
        method: 'get',
        url: "/api/role/all"
      }).then((res) => {
        this.loading = true;
        if (res.data.code === 0) {
          this.options=res.data.data
        }
      }).catch((error) => {
        console.error(error)
      });
    }
  },
};
</script>
