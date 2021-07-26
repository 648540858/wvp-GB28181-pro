import axios from 'axios';

class MediaServer{

  constructor() {
    this.$axios = axios;
  }

  getMediaServerList(callback){
    this.$axios({
      method: 'get',
      url:`/api/server/media_server/online/list`,
    }).then(function (res) {
      if (typeof (callback) == "function") callback(res.data)
    }).catch(function (error) {
      console.log(error);
    });
  }

  getMediaServer(id, callback){
    this.$axios({
      method: 'get',
      url:`/api/server/media_server/one/` + id,
    }).then(function (res) {
      if (typeof (callback) == "function") callback(res.data)
    }).catch(function (error) {
      console.log(error);
    });
  }
}

export default MediaServer;
