package com.genersoft.iot.vmp.gb28181.bean;

import com.genersoft.iot.vmp.gb28181.utils.XmlUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.dom4j.Element;

/**
 * 基础配置
 */
@Data
@Schema(description = "基础配置")
public class BasicParam implements DeviceConfigAware {

    @Schema(description = "设备ID")
    private String deviceId;

    @Schema(description = "通道ID，如果时对设备配置直接设置同设备ID一样即可")
    private String channelId;

    @Schema(description = "名称")
    private String name;

    @Schema(description = "注册过期时间")
    private String expiration;

    @Schema(description = "心跳间隔时间")
    private Integer heartBeatInterval;

    @Schema(description = "心跳超时次数")
    private Integer heartBeatCount;

    @Schema(description = "定位功能支持情况。取值:0-不支持;1-支持 GPS定位;2-支持北斗定位(可选,默认取值为0)，" +
            "用于接受配置查询结果， 基础配置时无效")
    private Integer positionCapability;

    @Schema(description = "经度(可选)，用于接受配置查询结果， 基础配置时无效")
    private Double longitude;

    @Schema(description = "纬度(可选)，用于接受配置查询结果， 基础配置时无效")
    private Double latitude;

    public static BasicParam getInstance(String name, String expiration, Integer heartBeatInterval, Integer heartBeatCount) {
        BasicParam basicParam = new BasicParam();
        basicParam.setName(name);
        basicParam.setExpiration(expiration);
        basicParam.setHeartBeatInterval(heartBeatInterval);
        basicParam.setHeartBeatCount(heartBeatCount);
        return basicParam;
    }

    @Override
    public String configType() {
        return "BasicParam";
    }

    @Override
    public void fromXml(Element element) {
        setName(XmlUtil.getText(element, "Name"));
        setExpiration(XmlUtil.getText(element, "Expiration"));
        setHeartBeatInterval(XmlUtil.getInteger(element, "HeartBeatInterval"));
        setHeartBeatCount(XmlUtil.getInteger(element, "HeartBeatCount"));
        setPositionCapability(XmlUtil.getInteger(element, "PositionCapability"));
        setLongitude(XmlUtil.getDouble(element, "Longitude"));
        setLatitude(XmlUtil.getDouble(element, "Latitude"));
    }
}
