<template>
  <div id="mapContainer" style="width: 100%;height: 100%;"></div>
</template>

<script>
import 'ol/ol.css';
import Map from 'ol/Map';
import OSM from 'ol/source/OSM';
import XYZ from 'ol/source/XYZ';
import TileLayer from 'ol/layer/Tile';
import View from 'ol/View';
import {get as getProj, fromLonLat} from 'ol/proj';
import {ZoomSlider, Zoom} from 'ol/control';

let olMap = null;

export default {
    name: 'MapComponent',
    data() {
        return {


        };
    },
    created(){
      this.$nextTick(() => {
        this.init();
      })

    },
    props: [],
    mounted () {

    },
    methods: {
      init(){

        let center = fromLonLat([116.41020, 39.915119]);
        if (mapParam.center) {
          center = fromLonLat(mapParam.center);
        }
        const view = new View({
          center: center,
          zoom:  mapParam.zoom || 10,
          projection: this.projection,
          maxZoom: mapParam.maxZoom || 19,
          minZoom: mapParam.minZoom || 1,
        });
        let tileLayer = null;
        if (mapParam.tilesUrl) {
          tileLayer = new TileLayer({
            source: new XYZ({
              projection: getProj("EPSG:3857"),
              wrapX: false,
              tileSize: 256 || mapParam.tileSize,
              url: mapParam.tilesUrl
            })
          })
        }else {
          tileLayer = new TileLayer({
            preload: 4,
            source: new OSM(),
          })
        }
        olMap = new Map({
          target: "mapContainer", // 容器ID
          layers:  [tileLayer], // 默认图层
          view: view,  // 视图
          controls:[   // 控件
            // new ZoomSlider(),
            new Zoom(),
          ] ,
        })
      },
      setCenter(point){

      },
      zoomIn(zoom){

      },
      zoomOut(zoom){

      },
      centerAndZoom(point,zoom,callback){

      },
      panTo(point){

      },
      openInfoBox(){

      },
      closeInfoBox(){

      },
      addLayer(){

      },
      removeLayer(){

      }



    },
    destroyed() {
      // if (this.jessibuca) {
      //   this.jessibuca.destroy();
      // }
      // this.playing = false;
      // this.loaded = false;
      // this.performance = "";
    },
}
</script>

<style>

</style>
