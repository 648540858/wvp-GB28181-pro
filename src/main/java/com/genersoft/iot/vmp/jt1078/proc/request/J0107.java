package com.genersoft.iot.vmp.jt1078.proc.request;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import com.genersoft.iot.vmp.jt1078.bean.*;
import com.genersoft.iot.vmp.jt1078.bean.common.ConfigAttribute;
import com.genersoft.iot.vmp.jt1078.bean.config.CameraTimer;
import com.genersoft.iot.vmp.jt1078.bean.config.CollisionAlarmParams;
import com.genersoft.iot.vmp.jt1078.bean.config.GnssPositioningMode;
import com.genersoft.iot.vmp.jt1078.bean.config.IllegalDrivingPeriods;
import com.genersoft.iot.vmp.jt1078.controller.JT1078Controller;
import com.genersoft.iot.vmp.jt1078.proc.Header;
import com.genersoft.iot.vmp.jt1078.proc.response.J8001;
import com.genersoft.iot.vmp.jt1078.proc.response.Rs;
import com.genersoft.iot.vmp.jt1078.service.Ijt1078Service;
import com.genersoft.iot.vmp.jt1078.session.Session;
import com.genersoft.iot.vmp.jt1078.session.SessionManager;
import com.genersoft.iot.vmp.jt1078.util.BCDUtil;
import io.netty.buffer.ByteBuf;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEvent;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 查询终端属性应答
 *
 */
@MsgId(id = "0107")
public class J0107 extends Re {

    private final static Logger logger = LoggerFactory.getLogger(J0107.class);

    Integer respNo;
    Integer paramLength;

    @Override
    protected Rs decode0(ByteBuf buf, Header header, Session session) {
        respNo = buf.readUnsignedShort();
        paramLength = (int) buf.readUnsignedByte();
        if (paramLength <= 0) {
            return null;
        }
        JTDeviceAttribute deviceAttribute = new JTDeviceAttribute();

        deviceAttribute.setType(JTDeviceType.getInstance(buf.getUnsignedShort(0)));

        byte[] bytes5 = new byte[5];
        buf.getBytes(2, bytes5);
        deviceAttribute.setMakerId(new String(bytes5).trim());

        byte[] bytes20 = new byte[20];
        buf.getBytes(7, bytes20);
        deviceAttribute.setDeviceModel(new String(bytes20).trim());

        byte[] bytes7 = new byte[7];
        buf.getBytes(37, bytes7);
        deviceAttribute.setTerminalId(new String(bytes7).trim());

        byte[] iccIdBytes = new byte[10];
        buf.getBytes(67, iccIdBytes);
        deviceAttribute.setIccId(BCDUtil.transform(iccIdBytes));

        int n = buf.getUnsignedByte(77);
        byte[] hardwareVersionBytes = new byte[n];
        buf.getBytes(78, hardwareVersionBytes);
        try {
            deviceAttribute.setHardwareVersion(new String(hardwareVersionBytes, "GBK").trim());
        } catch (UnsupportedEncodingException e) {
            logger.error("[查询终端属性应答] 读取硬件版本失败" , e);
        }

        int m = buf.getUnsignedByte(78 + n);
        byte[] firmwareVersionBytes = new byte[m];
        buf.getBytes(79 + n, firmwareVersionBytes);
        try {
            deviceAttribute.setFirmwareVersion(new String(firmwareVersionBytes, "GBK").trim());
        } catch (UnsupportedEncodingException e) {
            logger.error("[查询终端属性应答] 读取固件版本失败" , e);
        }

        deviceAttribute.setGnssAttribute(JGnssAttribute.getInstance(buf.getUnsignedByte(79 + n + m)));
        deviceAttribute.setCommunicationModuleAttribute(JCommunicationModuleAttribute.getInstance(buf.getUnsignedByte(80 + n + m)));
        System.out.println(deviceAttribute);
        List<String> allRequestKey = SessionManager.INSTANCE.getAllRequestKey();
        String prefix = String.join("_", header.getTerminalId().replaceFirst("^0*", ""), "0107");
        for (String key : allRequestKey) {
            if (key.startsWith(prefix)) {
                SessionManager.INSTANCE.response(key, deviceAttribute);
            }
        }

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
