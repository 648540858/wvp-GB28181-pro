package com.genersoft.iot.vmp.jt1078.proc.request;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import com.genersoft.iot.vmp.jt1078.bean.JTDevice;
import com.genersoft.iot.vmp.jt1078.bean.JTDeviceConfig;
import com.genersoft.iot.vmp.jt1078.bean.common.ConfigAttribute;
import com.genersoft.iot.vmp.jt1078.proc.Header;
import com.genersoft.iot.vmp.jt1078.proc.response.J8001;
import com.genersoft.iot.vmp.jt1078.proc.response.Rs;
import com.genersoft.iot.vmp.jt1078.service.Ijt1078Service;
import com.genersoft.iot.vmp.jt1078.session.Session;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import org.springframework.context.ApplicationEvent;

import java.lang.reflect.Field;
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
        System.err.println("应答流水号： " + respNo);
        paramLength = (int) buf.readUnsignedByte();
        System.err.println("参数项个数： " + paramLength);
        if (paramLength <= 0) {
            return null;
        }
        JTDeviceConfig deviceConfig = new JTDeviceConfig();
        Field[] fields = deviceConfig.getClass().getDeclaredFields();
        Map<Long, Field> allFieldMap = new HashMap<>();
        for (Field field : fields) {
            field.setAccessible(true);
            ConfigAttribute configAttribute = field.getAnnotation(ConfigAttribute.class);
            if (configAttribute != null) {
                allFieldMap.put(configAttribute.id(), field);
            }
        }
        System.out.println("========");
        for (int i = 0; i < paramLength; i++) {
            long id = buf.readUnsignedInt();
            short length = buf.readUnsignedByte();
            if (allFieldMap.containsKey(id)) {
                Field field = allFieldMap.get(id);
                field.setAccessible(true);
                try {
                    switch (field.getGenericType().toString()) {
                        case "class java.lang.Long":
                            field.set(deviceConfig, buf.readUnsignedInt());
                            continue;
                        case "class java.lang.String":
                            String val = buf.readCharSequence(length, Charset.forName("GBK")).toString().trim();
                            field.set(deviceConfig, val);
                            continue;
                        default:
                            System.err.println(field.getGenericType());
                            continue;
                    }
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        System.out.println(deviceConfig.toString());

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
