package com.genersoft.iot.vmp.jt1078.proc.request;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import com.genersoft.iot.vmp.jt1078.bean.*;
import com.genersoft.iot.vmp.jt1078.proc.Header;
import com.genersoft.iot.vmp.jt1078.proc.response.J8001;
import com.genersoft.iot.vmp.jt1078.proc.response.Rs;
import com.genersoft.iot.vmp.jt1078.service.Ijt1078Service;
import com.genersoft.iot.vmp.jt1078.session.Session;
import com.genersoft.iot.vmp.jt1078.session.SessionManager;
import com.genersoft.iot.vmp.jt1078.util.BCDUtil;
import io.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEvent;

import java.nio.charset.Charset;

/**
 * 查询终端属性应答
 *
 */
@MsgId(id = "0107")
public class J0107 extends Re {

    @Override
    protected Rs decode0(ByteBuf buf, Header header, Session session) {

        JTDeviceAttribute deviceAttribute = new JTDeviceAttribute();

        deviceAttribute.setType(JTDeviceType.getInstance(buf.readUnsignedShort()));

        deviceAttribute.setMakerId(buf.readCharSequence(5, Charset.forName("GBK")).toString().trim());

        deviceAttribute.setDeviceModel(buf.readCharSequence(20, Charset.forName("GBK")).toString().trim());
        if (header.is2019Version()) {
            buf.readCharSequence(10, Charset.forName("GBK"));
        }

        deviceAttribute.setTerminalId(buf.readCharSequence(7, Charset.forName("GBK")).toString().trim());
        if (header.is2019Version()) {
            buf.readCharSequence(23, Charset.forName("GBK"));
        }

        byte[] bytes = new byte[10];
        buf.readBytes(bytes);
        deviceAttribute.setIccId(BCDUtil.transform(bytes));

        int hardwareVersionLength = buf.readUnsignedByte();
        deviceAttribute.setHardwareVersion(buf.readCharSequence(hardwareVersionLength, Charset.forName("GBK")).toString().trim());

        int firmwareVersionLength = buf.readUnsignedByte();
        deviceAttribute.setFirmwareVersion(buf.readCharSequence(firmwareVersionLength, Charset.forName("GBK")).toString().trim());

        deviceAttribute.setGnssAttribute(JTGnssAttribute.getInstance(buf.readUnsignedByte()));
        deviceAttribute.setCommunicationModuleAttribute(JTCommunicationModuleAttribute.getInstance(buf.readUnsignedByte()));

        SessionManager.INSTANCE.response(header.getPhoneNumber(), "0107", null, deviceAttribute);
        return null;
    }

    @Override
    protected Rs handler(Header header, Session session, Ijt1078Service service) {
        J8001 j8001 = new J8001();
        j8001.setRespNo(header.getSn());
        j8001.setRespId(header.getMsgId());
        j8001.setResult(J8001.SUCCESS);
        return j8001;
    }

    @Override
    public ApplicationEvent getEvent() {
        return null;
    }
}
