package com.genersoft.iot.vmp.jt1078.proc.response;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import com.genersoft.iot.vmp.jt1078.bean.JTPhoneBookContact;
import com.genersoft.iot.vmp.jt1078.bean.JTVehicleControl;
import com.genersoft.iot.vmp.jt1078.bean.common.ConfigAttribute;
import com.genersoft.iot.vmp.jt1078.bean.config.JTDeviceSubConfig;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 车辆控制
 */
@MsgId(id = "8500")
public class J8500 extends Rs {

    /**
     * 控制类型
     */
    private JTVehicleControl vehicleControl;


    @Override
    public ByteBuf encode() {
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeShort((short)(vehicleControl.getLength() & 0xffff));

        Field[] fields = vehicleControl.getClass().getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            Object value = null;
            try {
                value = field.get(vehicleControl);
                if (value == null) {
                    continue;
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }

            ConfigAttribute configAttribute = field.getAnnotation(ConfigAttribute.class);
            if (configAttribute == null) {
                continue;
            }
            buffer.writeShort((short)(configAttribute.id() & 0xffff));
            switch (configAttribute.type()) {
                case "Byte":
                    field.setAccessible(true);
                    buffer.writeByte((int)value);
                    continue;
            }
        }

        return buffer;
    }

    public JTVehicleControl getVehicleControl() {
        return vehicleControl;
    }

    public void setVehicleControl(JTVehicleControl vehicleControl) {
        this.vehicleControl = vehicleControl;
    }
}
