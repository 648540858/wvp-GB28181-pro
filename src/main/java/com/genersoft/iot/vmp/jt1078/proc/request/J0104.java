package com.genersoft.iot.vmp.jt1078.proc.request;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import com.genersoft.iot.vmp.jt1078.bean.JTDevice;
import com.genersoft.iot.vmp.jt1078.bean.JTDeviceConfig;
import com.genersoft.iot.vmp.jt1078.bean.common.ConfigAttribute;
import com.genersoft.iot.vmp.jt1078.bean.config.IllegalDrivingPeriods;
import com.genersoft.iot.vmp.jt1078.proc.Header;
import com.genersoft.iot.vmp.jt1078.proc.response.J8001;
import com.genersoft.iot.vmp.jt1078.proc.response.Rs;
import com.genersoft.iot.vmp.jt1078.service.Ijt1078Service;
import com.genersoft.iot.vmp.jt1078.session.Session;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import org.springframework.context.ApplicationEvent;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

/**
 * 查询终端参数应答
 *
 */
@MsgId(id = "0104")
public class J0104 extends Re {

    Integer respNo;
    Integer paramLength;

    @Override
    protected Rs decode0(ByteBuf buf, Header header, Session session) {
        respNo = buf.readUnsignedShort();
        paramLength = (int) buf.readUnsignedByte();
        if (paramLength <= 0) {
            return null;
        }
        JTDeviceConfig deviceConfig = new JTDeviceConfig();
        Field[] fields = deviceConfig.getClass().getDeclaredFields();
        Map<Long, Field> allFieldMap = new HashMap<>();
        Map<Long, ConfigAttribute> allConfigAttributeMap = new HashMap<>();
        for (Field field : fields) {
            ConfigAttribute configAttribute = field.getAnnotation(ConfigAttribute.class);
            if (configAttribute != null) {
                allFieldMap.put(configAttribute.id(), field);
                allConfigAttributeMap.put(configAttribute.id(), configAttribute);
            }
        }
        for (int i = 0; i < paramLength; i++) {
            long id = buf.readUnsignedInt();
            if (!allFieldMap.containsKey(id)) {
                continue;
            }
            short length = buf.readUnsignedByte();
            Field field = allFieldMap.get(id);
            try {
                Method method = deviceConfig.getClass().getMethod("set" + field.getName().toLowerCase());
                switch (allConfigAttributeMap.get(id).type()) {
                    case "Long":
                        field.set(deviceConfig, buf.readUnsignedInt());
                        method.invoke(deviceConfig, buf.readUnsignedInt());
                        continue;
                    case "String":
                        String val = buf.readCharSequence(length, Charset.forName("GBK")).toString().trim();
                        field.set(deviceConfig, val);
                        continue;
                        case "IllegalDrivingPeriods":
                            IllegalDrivingPeriods illegalDrivingPeriods = new IllegalDrivingPeriods();
                            int startHour = buf.readUnsignedByte();
                            int startMinute = buf.readUnsignedByte();
                            int stopHour = buf.readUnsignedByte();
                            int stopMinute = buf.readUnsignedByte();
                            illegalDrivingPeriods.setStartTime(startHour + ":" + startMinute);
                            illegalDrivingPeriods.setEndTime(stopHour + ":" + stopMinute);
                            continue;
                    default:
                            System.err.println(field.getGenericType().getTypeName());
                        continue;
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
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
