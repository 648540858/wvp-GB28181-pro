import axios from 'axios';

class DeviceService{

  constructor() {
    this.$axios = axios;
  }

  getDeviceList(currentPage, count, callback, errorCallback){
    this.$axios({
      method: 'get',
      url:`/api/device/query/devices`,
      params: {
        page: currentPage,
        count: count
      }
    }).then((res) => {
      if (typeof (callback) == "function") callback(res.data)
    }).catch((error) => {
      console.log(error);
      if (typeof (errorCallback) == "function") errorCallback(error)
    });
  }
  getAllDeviceList(callback, errorCallback) {
    let currentPage = 1;
    let count = 100;
    let deviceList = []
    this.getAllDeviceListIteration(deviceList, currentPage, count, (data) => {
      if (typeof (callback) == "function") callback(data)
    }, errorCallback)
  }

  getAllDeviceListIteration(deviceList, currentPage, count, callback, errorCallback) {
    this.getDeviceList(currentPage, count, (data) => {
      if (data.list) {
        deviceList = deviceList.concat(data.list);
        if (deviceList.length < data.total) {
          currentPage ++
          this.getAllDeviceListIteration(deviceList, currentPage, count, callback, errorCallback)
        }else {
          if (typeof (callback) == "function") callback(deviceList)
        }
      }
    }, errorCallback)
  }


  getAllChannel(isCatalog, catalogUnderDevice, deviceId, callback, errorCallback) {
    let currentPage = 1;
    let count = 100;
    let catalogList = []
    this.getAllChannelIteration(isCatalog, catalogUnderDevice, deviceId, catalogList, currentPage, count, callback, errorCallback)
  }

  getAllChannelIteration(isCatalog, catalogUnderDevice, deviceId, catalogList, currentPage, count, callback, errorCallback) {
    this.getChanel(isCatalog, catalogUnderDevice, deviceId, currentPage, count, (data) => {
      if (data.list) {
        catalogList = catalogList.concat(data.list);
        if (catalogList.length < data.total) {
          currentPage ++
          this.getAllChannelIteration(isCatalog,catalogUnderDevice, deviceId, catalogList, currentPage, count, callback, errorCallback)
        }else {
          console.log(1)
          if (typeof (callback) == "function") callback(catalogList)
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


  getAllSubChannel(isCatalog, deviceId, channelId, callback, errorCallback) {
    let currentPage = 1;
    let count = 100;
    let catalogList = []
    this.getAllSubChannelIteration(isCatalog, deviceId, channelId, catalogList, currentPage, count, callback, errorCallback)
  }

  getAllSubChannelIteration(isCatalog, deviceId,channelId, catalogList, currentPage, count, callback, errorCallback) {
    this.getSubChannel(isCatalog, deviceId, channelId, currentPage, count, (data) => {
      if (data.list) {
        catalogList = catalogList.concat(data.list);
        if (catalogList.length < data.total) {
          currentPage ++
          this.getAllSubChannelIteration(isCatalog, deviceId, channelId, catalogList, currentPage, count, callback, errorCallback)
        }else {
          if (typeof (callback) == "function") callback(catalogList)
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
}

export default DeviceService;
