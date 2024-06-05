package com.genersoft.iot.vmp.jt1078.proc.request;

import com.genersoft.iot.vmp.common.CivilCodePo;
import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import com.genersoft.iot.vmp.jt1078.bean.JTDevice;
import com.genersoft.iot.vmp.jt1078.event.RegisterEvent;
import com.genersoft.iot.vmp.jt1078.proc.Header;
import com.genersoft.iot.vmp.jt1078.proc.response.J8100;
import com.genersoft.iot.vmp.jt1078.proc.response.Rs;
import com.genersoft.iot.vmp.jt1078.service.Ijt1078Service;
import com.genersoft.iot.vmp.jt1078.session.Session;
import com.genersoft.iot.vmp.utils.CivilCodeUtil;
import io.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEvent;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
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

    private final static Logger log = LoggerFactory.getLogger(J0100.class);
    private JTDevice device;

    @Override
    protected Rs decode0(ByteBuf buf, Header header, Session session) {
        Short version = header.getVersion();
        device = new JTDevice();
        device.setProvinceId(buf.readUnsignedShort() + "");
        if (version >= 1) {
            device.setCityId(buf.readUnsignedShort() + "");
            // decode as 2019
            device.setMakerId(buf.readCharSequence(11, Charset.forName("GBK"))
                    .toString().trim());

            device.setDeviceModel(buf.readCharSequence(30, Charset.forName("GBK"))
                    .toString().trim());

            device.setDeviceId(buf.readCharSequence(30, Charset.forName("GBK"))
                    .toString().trim());

            device.setPlateColor(buf.readByte());
            device.setPlateNo(buf.readCharSequence(buf.readableBytes(), Charset.forName("GBK"))
                    .toString().trim());
        } else {
            // decode as 2013
            device.setCityId(buf.readUnsignedShort() + "");
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
        JTDevice deviceInDb = service.getDevice(header.getTerminalPhoneNumber());
        if (deviceInDb != null) {
            j8100.setResult(J8100.SUCCESS);
            String authenticationCode = UUID.randomUUID().toString();
            j8100.setCode(authenticationCode);
            deviceInDb.setAuthenticationCode(authenticationCode);
            deviceInDb.setStatus(true);
            deviceInDb.setProvinceId(device.getProvinceId());
            CivilCodePo provinceCivilCodePo = CivilCodeUtil.INSTANCE.get(device.getProvinceId());
            if (provinceCivilCodePo != null) {
                deviceInDb.setProvinceText(provinceCivilCodePo.getName());
            }
            deviceInDb.setCityId(device.getCityId());
            CivilCodePo cityCivilCodePo = CivilCodeUtil.INSTANCE.get(device.getProvinceId() +
                    String.format("%04d", Integer.parseInt(device.getCityId())));
            if (cityCivilCodePo != null) {
                deviceInDb.setCityText(cityCivilCodePo.getName());
            }
            deviceInDb.setDeviceModel(device.getDeviceModel());
            deviceInDb.setMakerId(device.getMakerId());
            deviceInDb.setDeviceId(device.getDeviceId());
            // TODO 支持直接展示车牌颜色的描述
            deviceInDb.setPlateColor(device.getPlateColor());
            deviceInDb.setPlateNo(device.getPlateNo());
            service.updateDevice(deviceInDb);
            log.info("[JT-注册成功] 设备： {}", deviceInDb);
        }else {
            log.info("[JT-注册失败] 未授权设备： {}", header.getTerminalPhoneNumber());
            j8100.setResult(J8100.FAIL);
            // 断开连接，清理资源
            if (session.isRegistered()) {
                session.unregister();
            }
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
