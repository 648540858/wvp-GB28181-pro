package com.genersoft.iot.vmp.gb28181.bean;

import com.alibaba.fastjson2.JSONObject;
import lombok.Data;

@Data
public class SSEMessage<T> {
    private String event;
    private T data;

    public static SSEMessage<DeviceAlarm> getInstance(String event, DeviceAlarm data) {
        SSEMessage<DeviceAlarm> message = new SSEMessage<>();
        message.setEvent(event);
        message.setData(data);
        return message;
    }

    public String ecode(){
        return String.format("event:%s\ndata:%s\n", event, JSONObject.toJSONString(data));
    }
}
