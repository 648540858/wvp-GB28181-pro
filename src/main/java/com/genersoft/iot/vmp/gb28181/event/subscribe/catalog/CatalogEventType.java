package com.genersoft.iot.vmp.gb28181.event.subscribe.catalog;


public enum CatalogEventType{


    ON("ON"),               // 上线
    OFF("OFF"),             // 离线
    VLOST("VLOST"),         // 视频丢失
    DEFECT("DEFECT"),       // 故障
    ADD("ADD"),             // 增加
    DEL("DEL"),             // 删除
    UPDATE("UPDATE"),       // 更新
    ;

    private final String val;
    CatalogEventType(String val) {
        this.val = val;
    }

    public String getVal() {
        return val;
    }
}
