package com.genersoft.iot.vmp.media.zlm.dto.hook;

import com.genersoft.iot.vmp.media.zlm.dto.ServerKeepaliveData;

/**
 * zlm hook事件中的on_play事件的参数
 * @author lin
 */
public class OnServerKeepaliveHookParam extends HookParam{

    private ServerKeepaliveData data;

    public ServerKeepaliveData getData() {
        return data;
    }

    public void setData(ServerKeepaliveData data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "OnServerKeepaliveHookParam{" +
                "data=" + data +
                '}';
    }
}
