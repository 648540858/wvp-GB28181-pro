package com.genersoft.iot.vmp.jt1078.controller.bean;

import com.genersoft.iot.vmp.jt1078.bean.JTPhoneBookContact;
import com.genersoft.iot.vmp.jt1078.bean.JTTextSign;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@Schema(description = "设置电话本")
public class SetPhoneBookParam {

    @Schema(description = "终端手机号")
    private String phoneNumber;

    @Schema(description = "设置类型:\n" +
            "0: 删除终端上所有存储的联系人,\n" +
            "1: 表示更新电话本, 删除终端中已有全部联系人并追加消息中的联系人,\n" +
            "2: 表示追加电话本,\n" +
            "3: 表示修改电话本$以联系人为索引")
    private int type;

    @Schema(description = "联系人")
    private List<JTPhoneBookContact> phoneBookContactList;

    @Override
    public String toString() {
        return "SetPhoneBookParam{" +
                "设备手机号='" + phoneNumber + '\'' +
                ", type=" + type +
                ", phoneBookContactList=" + phoneBookContactList +
                '}';
    }
}
