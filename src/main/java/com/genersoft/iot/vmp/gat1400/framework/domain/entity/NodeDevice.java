package com.genersoft.iot.vmp.gat1400.framework.domain.entity;

import org.apache.commons.lang3.math.NumberUtils;

import cz.data.viid.framework.config.Constants;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class NodeDevice {

    @ApiModelProperty(value = "设备ID")
    private String deviceId;
    @ApiModelProperty(value = "交互协议")
    private String scheme = "http";
    @ApiModelProperty(value = "视图库地址")
    private String host;
    @ApiModelProperty(value = "视图库端口")
    private Integer port;
    @ApiModelProperty(value = "用户名")
    private String username;
    @ApiModelProperty("口令")
    private String password;
    @ApiModelProperty(value = "节点类别")
    private Category category;
    @ApiModelProperty(value = "节点源", notes = "视图库,设备")
    private Object origin;
    @ApiModelProperty("在线状态")
    private Boolean online = false;

    public String httpUrlBuilder() {
        return scheme + "://" + host + ":" + port;
    }

    public boolean isServer() {
        return Category.Server.equals(category);
    }

    public Boolean getOnline() {
        if (Category.Server.equals(category)) {
            VIIDServer server = (VIIDServer) origin;
            return online || server.serverIsOnline();
        }
        return online;
    }

    public VIIDServer originVIIDServer() {
        if (Category.Server.equals(category)) {
            return (VIIDServer) origin;
        }
        throw new RuntimeException("设备类型不正确");
    }

    public APEDevice originVIIDDevice() {
        if (Category.Device.equals(category)) {
            return (APEDevice) origin;
        }
        throw new RuntimeException("设备类型不正确");
    }

    public static NodeDevice fromServer(VIIDServer server) {
        NodeDevice node = new NodeDevice();
        node.setDeviceId(server.getServerId());
        node.setScheme(server.getScheme());
        node.setHost(server.getHost());
        node.setPort(server.getPort());
        node.setUsername(server.getUsername());
        node.setPassword(server.getAuthenticate());
        node.setCategory(Category.Server);
        node.setOrigin(server);
        node.setOnline(server.serverIsOnline());
        return node;
    }

    public static NodeDevice fromDevice(APEDevice device) {
        NodeDevice node = new NodeDevice();
        node.setDeviceId(device.getApeId());
        node.setHost(device.getIpAddr());
        node.setPort(NumberUtils.toInt(device.getPort(), 8120));
        node.setUsername(device.getUserId());
        node.setPassword(device.getPassword());
        node.setCategory(Category.Device);
        node.setOrigin(device);
        node.setOnline(Constants.DeviceStatus.Online.equalsValue(device.getIsOnline()));
        return node;
    }

    public enum Category {

        Server, Device;
    }
}
