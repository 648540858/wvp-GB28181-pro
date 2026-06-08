package com.genersoft.iot.vmp.gb28181.bean;


public class NotifyCatalogChannel {

    private Type type;

    private DeviceChannel channel;

    private String deviceId;


    public enum Type {
        ADD, DELETE, UPDATE, STATUS_CHANGED
    }


    public static NotifyCatalogChannel getInstance(Type type, DeviceChannel channel, String deviceId) {
        NotifyCatalogChannel notifyCatalogChannel = new NotifyCatalogChannel();
        notifyCatalogChannel.setType(type);
        notifyCatalogChannel.setChannel(channel);
        notifyCatalogChannel.setDeviceId(deviceId);
        return notifyCatalogChannel;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public DeviceChannel getChannel() {
        return channel;
    }

    public void setChannel(DeviceChannel channel) {
        this.channel = channel;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
}
