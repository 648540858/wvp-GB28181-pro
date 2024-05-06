package com.genersoft.iot.vmp.jt1078.proc.request;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import com.genersoft.iot.vmp.jt1078.bean.*;
import com.genersoft.iot.vmp.jt1078.proc.Header;
import com.genersoft.iot.vmp.jt1078.proc.response.J8001;
import com.genersoft.iot.vmp.jt1078.proc.response.Rs;
import com.genersoft.iot.vmp.jt1078.service.Ijt1078Service;
import com.genersoft.iot.vmp.jt1078.session.Session;
import com.genersoft.iot.vmp.jt1078.session.SessionManager;
import io.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * 查询区域或线路数据应答
 *
 */
@MsgId(id = "0608")
public class J0608 extends Re {

    private final static Logger log = LoggerFactory.getLogger(J0100.class);
    private JTPositionBaseInfo positionInfo;

    @Override
    protected Rs decode0(ByteBuf buf, Header header, Session session) {
        int type = buf.readByte();
        long dataLength = buf.readUnsignedInt();
        log.info("[JT-查询区域或线路数据应答]: 类型： {}， 数量： {}", type, dataLength);
        List<JTAreaOrRoute> areaOrRoutes = new ArrayList<>();
        if (dataLength == 0) {
            SessionManager.INSTANCE.response(header.getTerminalId(), "0608", null, areaOrRoutes);
            return null;
        }
        switch (type) {
            case 1:
                List<JTCircleArea> jtCircleAreas = new ArrayList<>();
                for (int i = 0; i < dataLength; i++) {
                    // 查询圆形区域数据
                    JTCircleArea jtCircleArea = JTCircleArea.decode(buf);
                    jtCircleAreas.add(jtCircleArea);
                }
                SessionManager.INSTANCE.response(header.getTerminalId(), "0608", null, jtCircleAreas);
                break;
            case 2:
                // 查询矩形区域数据
                List<JTRectangleArea> jtRectangleAreas = new ArrayList<>();
                for (int i = 0; i < dataLength; i++) {
                    // 查询圆形区域数据
                    JTRectangleArea jtRectangleArea = JTRectangleArea.decode(buf);
                    jtRectangleAreas.add(jtRectangleArea);
                }
                SessionManager.INSTANCE.response(header.getTerminalId(), "0608", null, jtRectangleAreas);
                break;
            case 3:
                // 查询多 边形区域数据
                List<JTPolygonArea> jtPolygonAreas = new ArrayList<>();
                for (int i = 0; i < dataLength; i++) {
                    // 查询圆形区域数据
                    JTPolygonArea jtRectangleArea = JTPolygonArea.decode(buf);
                    jtPolygonAreas.add(jtRectangleArea);
                }
                SessionManager.INSTANCE.response(header.getTerminalId(), "0608", null, jtPolygonAreas);
                break;
            case 4:
                // 查询线路数据
                // 查询多 边形区域数据
                JTPolygonArea jtPolygonArea = JTPolygonArea.decode(buf);
                SessionManager.INSTANCE.response(header.getTerminalId(), "0608", null, jtPolygonArea);
                break;
            default:
                break;
        }

        return null;
    }

    @Override
    protected Rs handler(Header header, Session session, Ijt1078Service service) {
        JTDevice deviceInDb = service.getDevice(header.getTerminalId());
        J8001 j8001 = new J8001();
        j8001.setRespNo(header.getSn());
        j8001.setRespId(header.getMsgId());
        if (deviceInDb == null) {
            j8001.setResult(J8001.FAIL);
        }else {
            // TODO 优化为发送异步事件，定时读取队列写入数据库
            deviceInDb.setLongitude(positionInfo.getLongitude());
            deviceInDb.setLatitude(positionInfo.getLatitude());
            service.updateDevice(deviceInDb);
            j8001.setResult(J8001.SUCCESS);
        }
        return j8001;
    }

    @Override
    public ApplicationEvent getEvent() {
        return null;
    }
}
