package com.genersoft.iot.vmp.vmanager.gb28181.dragZoom;

import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.transmit.callback.DeferredResultHolder;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommander;
import com.genersoft.iot.vmp.storager.IVideoManagerStorager;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * @author szy
 * @date 21:55 2022/1/15
 */
@Api(tags = "拉框控制")
@CrossOrigin
@RestController
@RequestMapping("/api/dragZoom")
public class DragZoomControl {

    private final static Logger logger = LoggerFactory.getLogger(DragZoomControl.class);

    @Autowired
    private SIPCommander cmder;

    @Autowired
    private IVideoManagerStorager storager;

    @Autowired
    private DeferredResultHolder resultHolder;

    /**
     * 拉框放大
     * @param deviceId 设备id
     * @param channelId 通道id
     * @param length 播放窗口长度像素值
     * @param width 播放窗口宽度像素值
     * @param midpointx 拉框中心的横轴坐标像素值
     * @param midpointy 拉框中心的纵轴坐标像素值
     * @param lengthx 拉框长度像素值
     * @param lengthy 拉框宽度像素值
     * @return
     */
    @ApiOperation("拉框放大")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "deviceId", value = "设备ID", required = true, dataTypeClass = String.class),
            @ApiImplicitParam(name = "channelId", value = "通道ID", dataTypeClass = String.class),
            @ApiImplicitParam(name = "length", value = "播放窗口长度像素值", required = true, dataTypeClass = Integer.class),
            @ApiImplicitParam(name = "width", value = "播放窗口宽度像素值", required = true, dataTypeClass = Integer.class),
            @ApiImplicitParam(name = "midpointx", value = "拉框中心的横轴坐标像素值", required = true, dataTypeClass = Integer.class),
            @ApiImplicitParam(name = "midpointy", value = "拉框中心的纵轴坐标像素值", required = true, dataTypeClass = Integer.class),
            @ApiImplicitParam(name = "lengthx", value = "拉框长度像素值", required = true, dataTypeClass = Integer.class),
            @ApiImplicitParam(name = "lengthy", value = "拉框宽度像素值", required = true, dataTypeClass = Integer.class),
    })
    @GetMapping("/dragzoomin")
    public ResponseEntity<String> dragZoomIn(@RequestParam String deviceId,
                                             @RequestParam(required = false) String channelId,
                                             @RequestParam int length,
                                             @RequestParam int width,
                                             @RequestParam int midpointx,
                                             @RequestParam int midpointy,
                                             @RequestParam int lengthx,
                                             @RequestParam int lengthy){
        if (logger.isDebugEnabled()) {
            logger.debug(String.format("设备拉框放大 API调用，deviceId：%s ，channelId：%s ，length：%d ，width：%d ，midpointx：%d ，midpointy：%d ，lengthx：%d ，lengthy：%d",deviceId, channelId, length, width, midpointx, midpointy,lengthx, lengthy));
        }
        Device device = storager.queryVideoDevice(deviceId);
        StringBuffer cmdXml = new StringBuffer(200);
        cmdXml.append("<DragZoomIn>\r\n");
        cmdXml.append("<Length>" + length+ "</Length>\r\n");
        cmdXml.append("<Width>" + width+ "</Width>\r\n");
        cmdXml.append("<MidPointX>" + midpointx+ "</MidPointX>\r\n");
        cmdXml.append("<MidPointY>" + midpointy+ "</MidPointY>\r\n");
        cmdXml.append("<LengthX>" + lengthx+ "</LengthX>\r\n");
        cmdXml.append("<LengthY>" + lengthy+ "</LengthY>\r\n");
        cmdXml.append("</DragZoomIn>\r\n");
        cmder.dragZoomCmd(device, channelId, cmdXml.toString());
        return new ResponseEntity<String>("success", HttpStatus.OK);
    }

    /**
     * 拉框缩小
     * @param deviceId 设备id
     * @param channelId 通道id
     * @param length 播放窗口长度像素值
     * @param width 播放窗口宽度像素值
     * @param midpointx 拉框中心的横轴坐标像素值
     * @param midpointy 拉框中心的纵轴坐标像素值
     * @param lengthx 拉框长度像素值
     * @param lengthy 拉框宽度像素值
     * @return
     */
    @ApiOperation("拉框缩小")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "deviceId", value = "设备ID", required = true, dataTypeClass = String.class),
            @ApiImplicitParam(name = "channelId", value = "通道ID", dataTypeClass = String.class),
            @ApiImplicitParam(name = "length", value = "播放窗口长度像素值", required = true, dataTypeClass = Integer.class),
            @ApiImplicitParam(name = "width", value = "播放窗口宽度像素值", required = true, dataTypeClass = Integer.class),
            @ApiImplicitParam(name = "midpointx", value = "拉框中心的横轴坐标像素值", required = true, dataTypeClass = Integer.class),
            @ApiImplicitParam(name = "midpointy", value = "拉框中心的纵轴坐标像素值", required = true, dataTypeClass = Integer.class),
            @ApiImplicitParam(name = "lengthx", value = "拉框长度像素值", required = true, dataTypeClass = Integer.class),
            @ApiImplicitParam(name = "lengthy", value = "拉框宽度像素值", required = true, dataTypeClass = Integer.class),
    })
    @GetMapping("/dragzoomout")
    public ResponseEntity<String> dragZoomOut(@RequestParam String deviceId,
                                              @RequestParam(required = false) String channelId,
                                              @RequestParam int length,
                                              @RequestParam int width,
                                              @RequestParam int midpointx,
                                              @RequestParam int midpointy,
                                              @RequestParam int lengthx,
                                              @RequestParam int lengthy){

        if (logger.isDebugEnabled()) {
            logger.debug(String.format("设备拉框缩小 API调用，deviceId：%s ，channelId：%s ，length：%d ，width：%d ，midpointx：%d ，midpointy：%d ，lengthx：%d ，lengthy：%d",deviceId, channelId, length, width, midpointx, midpointy,lengthx, lengthy));
        }
        Device device = storager.queryVideoDevice(deviceId);
        StringBuffer cmdXml = new StringBuffer(200);
        cmdXml.append("<DragZoomOut>\r\n");
        cmdXml.append("<Length>" + length+ "</Length>\r\n");
        cmdXml.append("<Width>" + width+ "</Width>\r\n");
        cmdXml.append("<MidPointX>" + midpointx+ "</MidPointX>\r\n");
        cmdXml.append("<MidPointY>" + midpointy+ "</MidPointY>\r\n");
        cmdXml.append("<LengthX>" + lengthx+ "</LengthX>\r\n");
        cmdXml.append("<LengthY>" + lengthy+ "</LengthY>\r\n");
        cmdXml.append("</DragZoomOut>\r\n");
        cmder.dragZoomCmd(device, channelId, cmdXml.toString());
        return new ResponseEntity<String>("success",HttpStatus.OK);
    }

}
