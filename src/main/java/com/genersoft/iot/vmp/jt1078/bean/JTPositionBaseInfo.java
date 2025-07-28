package com.genersoft.iot.vmp.jt1078.bean;

import com.genersoft.iot.vmp.jt1078.util.BCDUtil;
import io.netty.buffer.ByteBuf;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;

@Setter
@Getter
@Slf4j
@Schema(description = "位置基本信息")
public class JTPositionBaseInfo {

    /**
     * 报警标志
     */
    @Schema(description = "报警标志")
    private JTAlarmSign alarmSign;

    /**
     * 状态
     */
    @Schema(description = "状态")
    private JTStatus status;

    /**
     * 经度
     */
    @Schema(description = "经度")
    private Double longitude;

    /**
     * 纬度
     */
    @Schema(description = "纬度")
    private Double latitude;

    /**
     * 高程
     */
    @Schema(description = "高程")
    private Integer altitude;

    /**
     * 速度
     */
    @Schema(description = "速度")
    private Integer speed;

    /**
     * 方向
     */
    @Schema(description = "方向")
    private Integer direction;

    /**
     * 时间
     */
    @Schema(description = "时间")
    private String time;

    /**
     * 视频报警
     */
    @Schema(description = "视频报警")
    private JTVideoAlarm videoAlarm;

    public static JTPositionBaseInfo decode(ByteBuf buf) {
        JTPositionBaseInfo positionInfo = new JTPositionBaseInfo();
        if (buf.readableBytes() < 17) {
            log.error("[位置基本信息] 解码失败，长度不足: {}", buf.readableBytes());
            return positionInfo;
        }
        positionInfo.setAlarmSign(new JTAlarmSign(buf.readUnsignedInt()));

        positionInfo.setStatus(new JTStatus(buf.readUnsignedInt()));

        positionInfo.setLatitude(buf.readInt() * 0.000001D);
        positionInfo.setLongitude(buf.readInt() *  0.000001D);
        positionInfo.setAltitude(buf.readUnsignedShort());
        positionInfo.setSpeed(buf.readUnsignedShort());
        positionInfo.setDirection(buf.readUnsignedShort());
        byte[] timeBytes = new byte[6];
        buf.readBytes(timeBytes);
        positionInfo.setTime(BCDUtil.transform(timeBytes));
        return positionInfo;
    }


    public String toSimpleString() {
        return "简略位置汇报信息： " +
                " \n 经度：" + longitude +
                " \n 纬度：" + latitude +
                " \n 高程： " + altitude +
                " \n 速度： " + speed +
                " \n 方向： " + direction +
                " \n 时间： " + time +
                " \n";
    }

    @Override
    public String toString() {
        return "位置汇报信息： " +
                " \n 报警标志：" + alarmSign.toString() +
                " \n 状态：" + status.toString() +
                " \n 经度：" + longitude +
                " \n 纬度：" + latitude +
                " \n 高程： " + altitude +
                " \n 速度： " + speed +
                " \n 方向： " + direction +
                " \n 时间： " + time +
                " \n";
    }
}
