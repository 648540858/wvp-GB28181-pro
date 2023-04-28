package com.genersoft.iot.vmp.jt1078.proc.request;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import com.genersoft.iot.vmp.jt1078.proc.Header;
import com.genersoft.iot.vmp.jt1078.proc.response.J8100;
import com.genersoft.iot.vmp.jt1078.proc.response.Rs;
import com.genersoft.iot.vmp.jt1078.session.Session;
import io.netty.buffer.ByteBuf;

/**
 * 终端注册
 *
 * @author QingtaiJiang
 * @date 2023/4/27 18:06
 * @email qingtaij@163.com
 */
@MsgId(id = "0100")
public class J0100 extends Re {

    private int provinceId;

    private int cityId;

    private String makerId;

    private String deviceModel;

    private String deviceId;

    private int plateColor;

    private String plateNo;

    @Override
    protected Rs decode0(ByteBuf buf, Header header, Session session) {
        Short version = header.getVersion();
        provinceId = buf.readUnsignedShort();
        if (version > 1) {
            cityId = buf.readUnsignedShort();
            // decode as 2019
        } else {
            int i = buf.readUnsignedShort();
            // decode as 2013
        }
        return null;
    }

    @Override
    protected Rs handler(Header header, Session session) {
        J8100 j8100 = new J8100();
        j8100.setRespNo(header.getSn());
        j8100.setResult(J8100.SUCCESS);
        j8100.setCode("WVP_YYDS");
        return j8100;
    }
}
