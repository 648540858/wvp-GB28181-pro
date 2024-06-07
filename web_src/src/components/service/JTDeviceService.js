import axios from 'axios';

class JTDeviceService{

  constructor() {
    this.$axios = axios;
  }

  getDeviceList(currentPage, count, callback, errorCallback){
    this.$axios({
      method: 'get',
      url: `/api/jt1078/terminal/list`,
      params: {
        page: currentPage,
        count: count
      }
    }).then( (res)=> {
      if (typeof (callback) == "function") callback(res.data)
    }).catch( (error)=> {
      console.error(error);
      if (typeof (errorCallback) == "function") errorCallback(error)
    });
  }

  getDevice(deviceId, callback, errorCallback){
    this.$axios({
      method: 'get',
      url:`/api/jt1078/terminal/query`,
      params: {
        deviceId: deviceId
      },
    }).then((res) => {
      if (typeof (callback) == "function") callback(res.data)
    }).catch((error) => {
      console.log(error);
      if (typeof (errorCallback) == "function") errorCallback(error)
    });
  }

  deleteDevice(deviceId, callback, errorCallback){
    this.$axios({
      method: 'delete',
      url: '/api/jt1078/terminal/delete',
      params: {
        deviceId: deviceId
      }
    }).then((res) => {
      if (typeof (callback) == "function") callback(res.data)
    }).catch((error) => {
      if (typeof (errorCallback) == "function") errorCallback(error)
    });
  }



  getAllChannel(currentPage, count, searchSrt, deviceId, callback, endCallback, errorCallback) {
    this.$axios({
      method: 'get',
      url: `/api/jt1078/terminal/channel/list`,
      params: {
        page: currentPage,
        count: count,
        query: searchSrt,
        deviceId: deviceId,
      }
    }).then(function (res) {
      if (typeof (endCallback) == "function") endCallback(res.data)
    }).catch(function (error) {
      console.log(error);
      if (typeof (errorCallback) == "function") errorCallback(error)
    });
  }
}

export default JTDeviceService;
