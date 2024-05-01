package com.genersoft.iot.vmp.jt1078.controller.bean;

import com.genersoft.iot.vmp.jt1078.bean.JTPhoneBookContact;
import com.genersoft.iot.vmp.jt1078.bean.JTTextSign;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "设置电话本")
public class SetPhoneBookParam {

    @Schema(description = "设备")
    private String deviceId;

    @Schema(description = "设置类型:\n" +
            "0: 删除终端上所有存储的联系人,\n" +
            "1: 表示更新电话本, 删除终端中已有全部联系人并追加消息中的联系人,\n" +
            "2: 表示追加电话本,\n" +
            "3: 表示修改电话本$以联系人为索引")
    private int type;

    @Schema(description = "联系人")
    private List<JTPhoneBookContact> phoneBookContactList;

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public List<JTPhoneBookContact> getPhoneBookContactList() {
        return phoneBookContactList;
    }

    public void setPhoneBookContactList(List<JTPhoneBookContact> phoneBookContactList) {
        this.phoneBookContactList = phoneBookContactList;
    }

    @Override
    public String toString() {
        return "SetPhoneBookParam{" +
                "deviceId='" + deviceId + '\'' +
                ", type=" + type +
                ", phoneBookContactList=" + phoneBookContactList +
                '}';
    }
}
