package com.genersoft.iot.vmp.jt1078.proc.request;

import com.genersoft.iot.vmp.common.CivilCodePo;
import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import com.genersoft.iot.vmp.jt1078.bean.JTDevice;
import com.genersoft.iot.vmp.jt1078.event.DeviceUpdateEvent;
import com.genersoft.iot.vmp.jt1078.proc.Header;
import com.genersoft.iot.vmp.jt1078.proc.response.J8100;
import com.genersoft.iot.vmp.jt1078.proc.response.Rs;
import com.genersoft.iot.vmp.jt1078.service.Ijt1078Service;
import com.genersoft.iot.vmp.jt1078.session.Session;
import com.genersoft.iot.vmp.utils.CivilCodeUtil;
import com.genersoft.iot.vmp.utils.DateUtil;
import io.netty.buffer.ByteBuf;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@MsgId(id = "0100")
public class J0100 extends Re {

    private JTDevice device;
    private JTDevice deviceForUpdate;

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

            device.setModel(buf.readCharSequence(30, Charset.forName("GBK"))
                    .toString().trim());

            device.setTerminalId(buf.readCharSequence(30, Charset.forName("GBK"))
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
            device.setModel(new String(bytes20).trim());

            byte[] bytes7 = new byte[7];
            buf.readBytes(bytes7);
            device.setTerminalId(new String(bytes7).trim());

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
        deviceForUpdate = service.getDevice(header.getPhoneNumber());
        if (deviceForUpdate != null) {
            j8100.setResult(J8100.SUCCESS);
            String authenticationCode = UUID.randomUUID().toString();
            j8100.setCode(authenticationCode);
            deviceForUpdate.setAuthenticationCode(authenticationCode);
            deviceForUpdate.setStatus(true);
            deviceForUpdate.setProvinceId(device.getProvinceId());
            deviceForUpdate.setRegisterTime(DateUtil.getNow());
            CivilCodePo provinceCivilCodePo = CivilCodeUtil.INSTANCE.get(device.getProvinceId());
            if (provinceCivilCodePo != null) {
                deviceForUpdate.setProvinceText(provinceCivilCodePo.getName());
            }
            deviceForUpdate.setCityId(device.getCityId());
            CivilCodePo cityCivilCodePo = CivilCodeUtil.INSTANCE.get(device.getProvinceId() +
                    String.format("%04d", Integer.parseInt(device.getCityId())));
            if (cityCivilCodePo != null) {
                deviceForUpdate.setCityText(cityCivilCodePo.getName());
            }
            deviceForUpdate.setModel(device.getModel());
            deviceForUpdate.setMakerId(device.getMakerId());
            deviceForUpdate.setTerminalId(device.getTerminalId());
            // TODO 支持直接展示车牌颜色的描述
            deviceForUpdate.setPlateColor(device.getPlateColor());
            deviceForUpdate.setPlateNo(device.getPlateNo());
            log.info("[JT-注册成功] 设备： {}", deviceForUpdate);
        }else {
            log.info("[JT-注册失败] 未授权设备： {}", header.getPhoneNumber());
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
        DeviceUpdateEvent registerEvent = new DeviceUpdateEvent(this);
        registerEvent.setDevice(deviceForUpdate);
        return registerEvent;
    }
}
