package com.genersoft.iot.vmp.jt1078.proc.request;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import com.genersoft.iot.vmp.jt1078.event.RegisterEvent;
import com.genersoft.iot.vmp.jt1078.proc.Header;
import com.genersoft.iot.vmp.jt1078.proc.response.J8100;
import com.genersoft.iot.vmp.jt1078.proc.response.Rs;
import com.genersoft.iot.vmp.jt1078.session.Session;
import io.netty.buffer.ByteBuf;
import org.springframework.context.ApplicationEvent;

import java.io.UnsupportedEncodingException;

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
        if (version >= 1) {
            cityId = buf.readUnsignedShort();
            // decode as 2019
            byte[] bytes11 = new byte[11];
            buf.readBytes(bytes11);
            makerId = new String(bytes11).trim();

            byte[] bytes30 = new byte[30];
            buf.readBytes(bytes30);
            deviceModel = new String(bytes30).trim();

            buf.readBytes(bytes30);
            deviceId = new String(bytes30).trim();

            plateColor = buf.readByte();
            byte[] plateColorBytes = new byte[buf.readableBytes()];
            buf.readBytes(plateColorBytes);
            try {
                plateNo = new String(plateColorBytes, "GBK").trim();
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        } else {
            int i = buf.readUnsignedShort();
            // decode as 2013
        }
        // 发送终端注册消息

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

    @Override
    public ApplicationEvent getEvent() {
        RegisterEvent registerEvent = new RegisterEvent(this);
        registerEvent.setProvinceId(provinceId);
        registerEvent.setCityId(cityId);
        registerEvent.setDeviceId(deviceId);
        registerEvent.setDeviceModel(deviceModel);
        registerEvent.setMakerId(makerId);
        registerEvent.setPlateColor(plateColor);
        registerEvent.setPlateNo(plateNo);
        return registerEvent;
    }
}
