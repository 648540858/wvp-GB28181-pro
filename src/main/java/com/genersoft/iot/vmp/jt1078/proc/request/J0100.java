package com.genersoft.iot.vmp.jt1078.proc.request;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import com.genersoft.iot.vmp.jt1078.bean.JTDevice;
import com.genersoft.iot.vmp.jt1078.event.RegisterEvent;
import com.genersoft.iot.vmp.jt1078.proc.Header;
import com.genersoft.iot.vmp.jt1078.proc.response.J8100;
import com.genersoft.iot.vmp.jt1078.proc.response.Rs;
import com.genersoft.iot.vmp.jt1078.service.Ijt1078Service;
import com.genersoft.iot.vmp.jt1078.session.Session;
import io.netty.buffer.ByteBuf;
import org.springframework.context.ApplicationEvent;

import java.io.UnsupportedEncodingException;
import java.util.UUID;

/**
 * 终端注册
 *
 * @author QingtaiJiang
 * @date 2023/4/27 18:06
 * @email qingtaij@163.com
 */
@MsgId(id = "0100")
public class J0100 extends Re {

    private JTDevice device;

    @Override
    protected Rs decode0(ByteBuf buf, Header header, Session session) {
        Short version = header.getVersion();
        device = new JTDevice();
        device.setProvinceId(buf.readUnsignedShort());
        if (version >= 1) {
            device.setCityId(buf.readUnsignedShort());
            // decode as 2019
            byte[] bytes11 = new byte[11];
            buf.readBytes(bytes11);
            device.setMakerId(new String(bytes11).trim());

            byte[] bytes30 = new byte[30];
            buf.readBytes(bytes30);
            device.setDeviceModel(new String(bytes30).trim());

            buf.readBytes(bytes30);
            device.setDeviceId(new String(bytes30).trim());

            device.setPlateColor(buf.readByte());
            byte[] plateColorBytes = new byte[buf.readableBytes()];
            buf.readBytes(plateColorBytes);
            try {
                device.setPlateNo(new String(plateColorBytes, "GBK").trim());
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        } else {
            // decode as 2013
            device.setCityId(buf.readUnsignedShort());
            // decode as 2019
            byte[] bytes5 = new byte[5];
            buf.readBytes(bytes5);
            device.setMakerId(new String(bytes5).trim());

            byte[] bytes20 = new byte[20];
            buf.readBytes(bytes20);
            device.setDeviceModel(new String(bytes20).trim());

            byte[] bytes7 = new byte[7];
            buf.readBytes(bytes7);
            device.setDeviceId(new String(bytes7).trim());

            device.setPlateColor(buf.readByte());
            byte[] plateColorBytes = new byte[buf.readableBytes()];
            buf.readBytes(plateColorBytes);
            try {
                device.setPlateNo(new String(plateColorBytes, "GBK").trim());
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    @Override
    protected Rs handler(Header header, Session session, Ijt1078Service service) {
        J8100 j8100 = new J8100();
        j8100.setRespNo(header.getSn());
        // 从数据库判断这个设备是否合法
        JTDevice deviceInDb = service.getDevice(header.getDevId());
        if (deviceInDb != null) {
            j8100.setResult(J8100.SUCCESS);
            String authenticationCode = UUID.randomUUID().toString();
            j8100.setCode(authenticationCode);
            deviceInDb.setAuthenticationCode(authenticationCode);
            service.updateDevice(deviceInDb);
        }else {
            j8100.setResult(J8100.FAIL);
            // TODO 断开连接，清理资源
        }
        return j8100;
    }

    @Override
    public ApplicationEvent getEvent() {
        RegisterEvent registerEvent = new RegisterEvent(this);
        registerEvent.setDevice(device);
        return registerEvent;
    }
}
