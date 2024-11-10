<template>
  <div id="log" style="width: 100%;height: 100%;">
    <operationsFoShowLog ref="recordVideoPlayer" :remoteUrl="removeUrl" ></operationsFoShowLog>
  </div>
</template>

<script>

import operationsFoShowLog from "./dialog/operationsFoShowLog.vue";

export default {
  name: 'log',
  components: {operationsFoShowLog},
  data() {
    return {
      loading: false,
      removeUrl: this.getURl(),
      winHeight: window.innerHeight - 220,
    };
  },
  created() {
    console.log("removeUrl11 == " + this.removeUrl)
  },
  methods: {
    getURl: function () {
      if (process.env.NODE_ENV !== 'development') {
        if (location.protocol === "https:") {
          return `wss://${window.location.host}/channel/log`;
        }else {
          return `ws://${window.location.host}/channel/log`
        }
      }else {
        return "ws://localhost:18080/channel/log";
      }
    },
  }
};
</script>
