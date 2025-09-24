package com.genersoft.iot.vmp.media.abl.bean.hook;


import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class OnPlayABLHookParam extends ABLHookParam{

    private String ip;
    private Integer port;
    private String params;

    @Override
    public String toString() {
        return "OnPlayABLHookParam{" +
                "ip='" + ip + '\'' +
                ", port=" + port +
                ", params='" + params + '\'' +
                '}';
    }
}
