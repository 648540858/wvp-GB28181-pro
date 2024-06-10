package com.genersoft.iot.vmp.jt1078.proc.request;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import com.genersoft.iot.vmp.jt1078.bean.JTDeviceConfig;
import com.genersoft.iot.vmp.jt1078.bean.common.ConfigAttribute;
import com.genersoft.iot.vmp.jt1078.bean.config.*;
import com.genersoft.iot.vmp.jt1078.proc.Header;
import com.genersoft.iot.vmp.jt1078.proc.response.J8001;
import com.genersoft.iot.vmp.jt1078.proc.response.Rs;
import com.genersoft.iot.vmp.jt1078.service.Ijt1078Service;
import com.genersoft.iot.vmp.jt1078.session.Session;
import com.genersoft.iot.vmp.jt1078.session.SessionManager;
import io.netty.buffer.ByteBuf;
import org.apache.commons.lang3.StringUtils;
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

                switch (allConfigAttributeMap.get(id).type()) {
                    case "Long":
                        Method methodForLong = deviceConfig.getClass().getDeclaredMethod("set" + StringUtils.capitalize(field.getName()), Long.class);
                        methodForLong.invoke(deviceConfig, buf.readUnsignedInt());
                        continue;
                    case "String":
                        Method methodForString = deviceConfig.getClass().getDeclaredMethod("set" + StringUtils.capitalize(field.getName()), String.class);
                        String val = buf.readCharSequence(length, Charset.forName("GBK")).toString().trim();
                        methodForString.invoke(deviceConfig, val);
                        continue;
                    case "Integer":
                        Method methodForInteger = deviceConfig.getClass().getDeclaredMethod("set" + StringUtils.capitalize(field.getName()), Integer.class);
                        methodForInteger.invoke(deviceConfig, buf.readUnsignedShort());
                        continue;
                    case "Short":
                        Method methodForShort = deviceConfig.getClass().getDeclaredMethod("set" + StringUtils.capitalize(field.getName()), Short.class);
                        methodForShort.invoke(deviceConfig, buf.readUnsignedByte());
                        continue;
                    case "IllegalDrivingPeriods":
                        JTIllegalDrivingPeriods illegalDrivingPeriods = new JTIllegalDrivingPeriods();
                        int startHour = buf.readUnsignedByte();
                        int startMinute = buf.readUnsignedByte();
                        int stopHour = buf.readUnsignedByte();
                        int stopMinute = buf.readUnsignedByte();
                        illegalDrivingPeriods.setStartTime(startHour + ":" + startMinute);
                        illegalDrivingPeriods.setEndTime(stopHour + ":" + stopMinute);
                        Method methodForIllegalDrivingPeriods = deviceConfig.getClass().getDeclaredMethod("set" + StringUtils.capitalize(field.getName()), JTIllegalDrivingPeriods.class);
                        methodForIllegalDrivingPeriods.invoke(deviceConfig, illegalDrivingPeriods);
                        continue;
                    case "CollisionAlarmParams":
                        JTCollisionAlarmParams collisionAlarmParams = new JTCollisionAlarmParams();
                        collisionAlarmParams.setCollisionAlarmTime(buf.readUnsignedByte());
                        collisionAlarmParams.setCollisionAcceleration(buf.readUnsignedByte());
                        Method methodForCollisionAlarmParams = deviceConfig.getClass().getDeclaredMethod("set" + StringUtils.capitalize(field.getName()), JTCollisionAlarmParams.class);
                        methodForCollisionAlarmParams.invoke(deviceConfig, collisionAlarmParams);
                        continue;
                    case "CameraTimer":
                        JTCameraTimer cameraTimer = new JTCameraTimer();
                        long cameraTimerContent = buf.readUnsignedInt();
                        cameraTimer.setSwitchForChannel1((cameraTimerContent & 1) == 1);
                        cameraTimer.setSwitchForChannel2((cameraTimerContent >>> 1 & 1) == 1);
                        cameraTimer.setSwitchForChannel3((cameraTimerContent >>> 2 & 1) == 1);
                        cameraTimer.setSwitchForChannel4((cameraTimerContent >>> 3 & 1) == 1);
                        cameraTimer.setSwitchForChannel5((cameraTimerContent >>> 4 & 1) == 1);
                        cameraTimer.setStorageFlagsForChannel1((cameraTimerContent >>> 7 & 1) == 1);
                        cameraTimer.setStorageFlagsForChannel2((cameraTimerContent >>> 8 & 1) == 1);
                        cameraTimer.setStorageFlagsForChannel3((cameraTimerContent >>> 9 & 1) == 1);
                        cameraTimer.setStorageFlagsForChannel4((cameraTimerContent >>> 10 & 1) == 1);
                        cameraTimer.setStorageFlagsForChannel5((cameraTimerContent >>> 11 & 1) == 1);
                        cameraTimer.setTimeUnit((cameraTimerContent >>> 15 & 1) == 1);
                        cameraTimer.setTimeInterval((int)cameraTimerContent >>> 16);
                        Method methodForCameraTimer = deviceConfig.getClass().getDeclaredMethod("set" + StringUtils.capitalize(field.getName()), JTCameraTimer.class);
                        methodForCameraTimer.invoke(deviceConfig, cameraTimer);
                        continue;
                    case "GnssPositioningMode":
                        JTGnssPositioningMode gnssPositioningMode = new JTGnssPositioningMode();
                        short gnssPositioningModeContent = buf.readUnsignedByte();
                        gnssPositioningMode.setGps((gnssPositioningModeContent& 1) == 1);
                        gnssPositioningMode.setBeidou((gnssPositioningModeContent >>> 1 & 1) == 1);
                        gnssPositioningMode.setGlonass((gnssPositioningModeContent >>> 2 & 1) == 1);
                        gnssPositioningMode.setGaLiLeo((gnssPositioningModeContent >>> 3 & 1) == 1);
                        Method methodForGnssPositioningMode = deviceConfig.getClass().getDeclaredMethod("set" + StringUtils.capitalize(field.getName()), JTGnssPositioningMode.class);
                        methodForGnssPositioningMode.invoke(deviceConfig, gnssPositioningMode);
                        continue;
                    case "VideoParam":
                        JTVideoParam videoParam = JTVideoParam.decode(buf);
                        Method methodForVideoParam = deviceConfig.getClass().getDeclaredMethod("set" + StringUtils.capitalize(field.getName()), JTVideoParam.class);
                        methodForVideoParam.invoke(deviceConfig, videoParam);
                        continue;
                    case "ChannelListParam":
                        JTChannelListParam channelListParam = JTChannelListParam.decode(buf);
                        Method methodForChannelListParam = deviceConfig.getClass().getDeclaredMethod("set" + StringUtils.capitalize(field.getName()), JTChannelListParam.class);
                        methodForChannelListParam.invoke(deviceConfig, channelListParam);
                    case "ChannelParam":
                        JTChannelParam channelParam = JTChannelParam.decode(buf);
                        Method methodForChannelParam = deviceConfig.getClass().getDeclaredMethod("set" + StringUtils.capitalize(field.getName()), JTChannelParam.class);
                        methodForChannelParam.invoke(deviceConfig, channelParam);
                        continue;
                    case "AlarmRecordingParam":
                        JTAlarmRecordingParam alarmRecordingParam = JTAlarmRecordingParam.decode(buf);
                        Method methodForAlarmRecordingParam = deviceConfig.getClass().getDeclaredMethod("set" + StringUtils.capitalize(field.getName()), JTAlarmRecordingParam.class);
                        methodForAlarmRecordingParam.invoke(deviceConfig, alarmRecordingParam);
                        continue;
                    case "VideoAlarmBit":
                        JTVideoAlarmBit videoAlarmBit = JTVideoAlarmBit.decode(buf);
                        Method methodForVideoAlarmBit = deviceConfig.getClass().getDeclaredMethod("set" + StringUtils.capitalize(field.getName()), JTVideoAlarmBit.class);
                        methodForVideoAlarmBit.invoke(deviceConfig, videoAlarmBit);
                        continue;
                    case "AnalyzeAlarmParam":
                        JTAnalyzeAlarmParam analyzeAlarmParam = JTAnalyzeAlarmParam.decode(buf);
                        Method methodForAnalyzeAlarmParam = deviceConfig.getClass().getDeclaredMethod("set" + StringUtils.capitalize(field.getName()), JTAnalyzeAlarmParam.class);
                        methodForAnalyzeAlarmParam.invoke(deviceConfig, analyzeAlarmParam);
                        continue;
                    case "AwakenParam":
                        JTAwakenParam awakenParamParam = JTAwakenParam.decode(buf);
                        Method methodForAwakenParam = deviceConfig.getClass().getDeclaredMethod("set" + StringUtils.capitalize(field.getName()), JTAwakenParam.class);
                        methodForAwakenParam.invoke(deviceConfig, awakenParamParam);
                        continue;
                    default:
                            System.err.println(field.getGenericType().getTypeName());
                        continue;
                }
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }
        SessionManager.INSTANCE.response(header.getPhoneNumber(), "0104", (long) respNo, deviceConfig);
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
