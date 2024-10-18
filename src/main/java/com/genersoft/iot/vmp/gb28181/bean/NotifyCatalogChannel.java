package com.genersoft.iot.vmp.gb28181.bean;


public class NotifyCatalogChannel {

    private Type type;

    private DeviceChannel channel;


    public enum Type {
        ADD, DELETE, UPDATE, STATUS_CHANGED
    }


    public static NotifyCatalogChannel getInstance(Type type, DeviceChannel channel) {
        NotifyCatalogChannel notifyCatalogChannel = new NotifyCatalogChannel();
        notifyCatalogChannel.setType(type);
        notifyCatalogChannel.setChannel(channel);
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
}
