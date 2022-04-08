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
      console.log(data)
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


  getAllCatalog(deviceId, callback, errorCallback) {
    let currentPage = 1;
    let count = 100;
    let catalogList = []
    this.getAllCatalogIteration(deviceId, catalogList, currentPage, count, callback, errorCallback)
  }

  getAllCatalogIteration(deviceId, catalogList, currentPage, count, callback, errorCallback) {
    this.getCatalog(deviceId, currentPage, count, (data) => {
      console.log(data)
      if (data.list) {
        catalogList = catalogList.concat(data.list);
        if (catalogList.length < data.total) {
          currentPage ++
          this.getAllCatalogIteration(deviceId, catalogList, currentPage, count, callback, errorCallback)
        }else {
          console.log(2222)
          if (typeof (callback) == "function") callback(catalogList)
        }
      }
    }, errorCallback)
  }
  getCatalog(deviceId, currentPage, count, callback, errorCallback) {
    this.$axios({
      method: 'get',
      url: `/api/device/query/devices/${deviceId}/channels`,
      params:{
        page: currentPage,
        count: count,
        query: "",
        online: "",
        channelType: true
      }
    }).then((res) =>{
      if (typeof (callback) == "function") callback(res.data)
    }).catch(errorCallback);
  }


  getAllSubCatalog(deviceId, channelId, callback, errorCallback) {
    let currentPage = 1;
    let count = 100;
    let catalogList = []
    this.getAllSubCatalogIteration(deviceId, channelId, catalogList, currentPage, count, callback, errorCallback)
  }

  getAllSubCatalogIteration(deviceId,channelId, catalogList, currentPage, count, callback, errorCallback) {
    this.getSubCatalog(deviceId, channelId, currentPage, count, (data) => {
      console.log(data)
      if (data.list) {
        catalogList = catalogList.concat(data.list);
        if (catalogList.length < data.total) {
          currentPage ++
          this.getAllSubCatalogIteration(deviceId, channelId, catalogList, currentPage, count, callback, errorCallback)
        }else {
          console.log(2222)
          if (typeof (callback) == "function") callback(catalogList)
        }
      }
    }, errorCallback)
  }
  getSubCatalog(deviceId, channelId, currentPage, count, callback, errorCallback) {
    this.$axios({
      method: 'get',
      url: `/api/device/query/sub_channels/${deviceId}/${channelId}/channels`,
      params:{
        page: currentPage,
        count: count,
        query: "",
        online: "",
        channelType: true
      }
    }).then((res) =>{
      if (typeof (callback) == "function") callback(res.data)
    }).catch(errorCallback);
  }
}

export default DeviceService;
