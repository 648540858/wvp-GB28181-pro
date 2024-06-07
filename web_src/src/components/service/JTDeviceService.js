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



  getAllChannel(isCatalog, catalogUnderDevice, deviceId, callback, endCallback, errorCallback) {
    let currentPage = 1;
    let count = 100;
    let catalogList = []
    this.getAllChannelIteration(isCatalog, catalogUnderDevice, deviceId, catalogList, currentPage, count, callback, endCallback, errorCallback)
  }

  getAllChannelIteration(isCatalog, catalogUnderDevice, deviceId, catalogList, currentPage, count, callback, endCallback, errorCallback) {
    this.getChanel(isCatalog, catalogUnderDevice, deviceId, currentPage, count, (data) => {
      if (data.list) {
        if (typeof (callback) == "function") callback(data.list)
        catalogList = catalogList.concat(data.list);
        if (catalogList.length < data.total) {
          currentPage ++
          this.getAllChannelIteration(isCatalog,catalogUnderDevice, deviceId, catalogList, currentPage, count, callback, errorCallback)
        }else {
          console.log(1)
          if (typeof (endCallback) == "function") endCallback(catalogList)
        }
      }
    }, errorCallback)
  }
  getChanel(isCatalog, catalogUnderDevice, deviceId, currentPage, count, callback, errorCallback) {
    this.$axios({
      method: 'get',
      url: `/api/device/query/devices/${deviceId}/channels`,
      params:{
        page: currentPage,
        count: count,
        query: "",
        online: "",
        channelType: isCatalog,
        catalogUnderDevice: catalogUnderDevice
      }
    }).then((res) =>{
      if (typeof (callback) == "function") callback(res.data)
    }).catch(errorCallback);
  }


  getAllSubChannel(isCatalog, deviceId, channelId, callback, endCallback, errorCallback) {
    let currentPage = 1;
    let count = 100;
    let catalogList = []
    this.getAllSubChannelIteration(isCatalog, deviceId, channelId, catalogList, currentPage, count, callback, endCallback, errorCallback)
  }

  getAllSubChannelIteration(isCatalog, deviceId,channelId, catalogList, currentPage, count, callback, endCallback, errorCallback) {
    this.getSubChannel(isCatalog, deviceId, channelId, currentPage, count, (data) => {
      if (data.list) {
        if (typeof (callback) == "function") callback(data.list)
        catalogList = catalogList.concat(data.list);
        if (catalogList.length < data.total) {
          currentPage ++
          this.getAllSubChannelIteration(isCatalog, deviceId, channelId, catalogList, currentPage, count, callback, endCallback, errorCallback)
        }else {
          if (typeof (endCallback) == "function") endCallback(catalogList)
        }
      }
    }, errorCallback)
  }
  getSubChannel(isCatalog, deviceId, channelId, currentPage, count, callback, errorCallback) {
    this.$axios({
      method: 'get',
      url: `/api/device/query/sub_channels/${deviceId}/${channelId}/channels`,
      params:{
        page: currentPage,
        count: count,
        query: "",
        online: "",
        channelType: isCatalog
      }
    }).then((res) =>{
      if (typeof (callback) == "function") callback(res.data)
    }).catch(errorCallback);
  }

  getTree(deviceId, parentId, onlyCatalog, callback, endCallback, errorCallback) {
    let currentPage = 1;
    let count = 100;
    let catalogList = []
    this.getTreeIteration(deviceId, parentId, onlyCatalog, catalogList, currentPage, count, callback, endCallback, errorCallback)
  }

  getTreeIteration(deviceId, parentId, onlyCatalog, catalogList, currentPage, count, callback, endCallback, errorCallback) {
    this.getTreeInfo(deviceId, parentId, onlyCatalog, currentPage, count, (data) => {
      if (data.code === 0 && data.data.list) {
        if (typeof (callback) == "function") callback(data.data.list)
        catalogList = catalogList.concat(data.data.list);
        if (catalogList.length < data.data.total) {
          currentPage ++
          this.getTreeIteration(deviceId, parentId, onlyCatalog, catalogList, currentPage, count, callback, endCallback, errorCallback)
        }else {
          if (typeof (endCallback) == "function") endCallback(catalogList)
        }
      }
    }, errorCallback)
  }
  getTreeInfo(deviceId, parentId, onlyCatalog, currentPage, count, callback, errorCallback) {
    if (onlyCatalog == null || typeof onlyCatalog === "undefined") {
      onlyCatalog = false;
    }
    this.$axios({
      method: 'get',
      url: `/api/device/query/tree/${deviceId}`,
      params:{
        page: currentPage,
        count: count,
        parentId: parentId,
        onlyCatalog: onlyCatalog
      }
    }).then((res) =>{
      if (typeof (callback) == "function") callback(res.data)
    }).catch(errorCallback);
  }
}

export default JTDeviceService;
