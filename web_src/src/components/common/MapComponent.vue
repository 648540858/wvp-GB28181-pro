<template>
  <div id="mapContainer" ref="mapContainer" style="width: 100%;height: 100%;"></div>
</template>

<script>
import 'ol/ol.css';
import Map from 'ol/Map';
import OSM from 'ol/source/OSM';
import XYZ from 'ol/source/XYZ';
import VectorSource from 'ol/source/Vector';
import Tile from 'ol/layer/Tile';
import VectorLayer from 'ol/layer/Vector';
import Style from 'ol/style/Style';
import Icon from 'ol/style/Icon';
import View from 'ol/View';
import Feature from 'ol/Feature';
import Overlay from 'ol/Overlay';
import {Point, LineString} from 'ol/geom';
import {get as getProj, fromLonLat} from 'ol/proj';
import {ZoomSlider, Zoom} from 'ol/control';
import {containsCoordinate} from 'ol/extent';

import {v4}  from 'uuid'

let olMap = null;

export default {
    name: 'MapComponent',
    data() {
        return {


        };
    },
    created(){
      this.$nextTick(() => {
        setTimeout(()=>{
          this.init()
        }, 100)
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
          tileLayer = new Tile({
            source: new XYZ({
              projection: getProj("EPSG:3857"),
              wrapX: false,
              tileSize: 256 || mapParam.tileSize,
              url: mapParam.tilesUrl
            })
          })
        }else {
          tileLayer = new Tile({
            preload: 4,
            source: new OSM(),
          })
        }
        olMap = new Map({
          target: this.$refs.mapContainer, // 容器ID
          layers:  [tileLayer], // 默认图层
          view: view,  // 视图
          controls:[   // 控件
            // new ZoomSlider(),
            new Zoom(),
          ] ,
        })
        console.log(3222)
      },
      setCenter(point){

      },
      zoomIn(zoom){

      },
      zoomOut(zoom){

      },
      centerAndZoom(point,zoom,callback){
        var zoom_ = olMap.getView().getZoom();
        zoom = zoom|| zoom_;
        var duration = 600;
        olMap.getView().setCenter(fromLonLat(point))
        olMap.getView().animate({
          zoom: zoom ,
          duration: duration
        });
      },
      panTo(point, zoom){
        let duration = 800;

        olMap.getView().cancelAnimations()
        olMap.getView().animate({
          center: fromLonLat(point),
          duration: duration
        });
        if (!containsCoordinate(olMap.getView().calculateExtent(), fromLonLat(point))) {
          olMap.getView().animate({
            zoom: olMap.getView().getZoom() - 1,
            duration: duration / 2
          }, {
            zoom: zoom || olMap.getView().getZoom(),
            duration: duration / 2
          });
        }

      },
      fit(layer){
        let extent = layer.getSource().getExtent();
        if (extent) {
          olMap.getView().fit(extent,{
            duration : 600,
            padding: [100, 100, 100, 100]
          });
        }


      },
      openInfoBox(position, content, offset){
        let id = v4()
        // let infoBox = document.createElement("div");
        // infoBox.innerHTML = content ;
        // infoBox.setAttribute("infoBoxId", id)
        let overlay = new Overlay({
          id:id,
          autoPan:true,
          autoPanAnimation:{
            duration: 250
          },
          element: content,
          positioning:"bottom-center",
          offset:offset,
          // className:overlayStyle.className
        });
        olMap.addOverlay(overlay);
        overlay.setPosition(fromLonLat(position));
        return id;
      },
      closeInfoBox(id){
        olMap.getOverlayById(id).setPosition(undefined)
        // olMap.removeOverlay(olMap.getOverlayById(id))
      },
      /**
       * 添加图层
       * @param data
       * [
       *     {
       *
       *         position: [119.1212,45,122],
       *             image: {
       *                 src:"/images/123.png",
       *                 anchor: [0.5, 0.5]
       *
       *             }
       *     }
       *
       * ]
       */
      addLayer(data, clickEvent){
        let style = new Style();
        if (data.length > 0) {
          let features = [];
          for (let i = 0; i < data.length; i++) {
            let feature = new Feature(new Point(fromLonLat(data[i].position)));
            feature.customData = data[i].data;
            let cloneStyle = style.clone()
            cloneStyle.setImage(new Icon({
              anchor: data[i].image.anchor,
              crossOrigin: 'Anonymous',
              src: data[i].image.src,
            }))
            feature.setStyle(cloneStyle)
            features.push(feature);
          }
          let source = new VectorSource();
          source.addFeatures(features);
          let vectorLayer = new VectorLayer({
            source:source,
            style:style,
            renderMode:"image",
            declutter: false
          })
          olMap.addLayer(vectorLayer)
          if (typeof clickEvent == "function") {
            olMap.on("click", (event)=>{
              vectorLayer.getFeatures(event.pixel).then((features)=>{
                if (features.length > 0) {
                  let items = []
                  for (let i = 0; i < features.length; i++) {
                    items.push(features[i].customData)
                  }
                  clickEvent(items)
                }
              })


            })
          }

          return vectorLayer;
        }
      },
      removeLayer(layer){
        olMap.removeLayer(layer)
      },

      addLineLayer(positions) {
        if (positions.length > 0) {
          let points = [];
          for (let i = 0; i < positions.length; i++) {
            points.push(fromLonLat(positions[i]));
          }
          let line = new LineString(points)
          let lineFeature = new Feature(line);

          let source = new VectorSource();
          source.addFeature(lineFeature);
          let vectorLayer = new VectorLayer({
            source: source,
          })
          olMap.addLayer(vectorLayer)
          return vectorLayer;
        }
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
