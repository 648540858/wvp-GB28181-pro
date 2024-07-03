package com.genersoft.iot.vmp.jt1078.proc.request;

import com.genersoft.iot.vmp.jt1078.proc.Header;
import com.genersoft.iot.vmp.jt1078.proc.response.Rs;
import com.genersoft.iot.vmp.jt1078.session.Session;
import io.netty.buffer.ByteBuf;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

/**
 * @author QingtaiJiang
 * @date 2023/4/27 18:50
 * @email qingtaij@163.com
 */
@Slf4j
public abstract class Re {

    protected abstract Rs decode0(ByteBuf buf, Header header, Session session);

    protected abstract Rs handler(Header header, Session session);

    public Rs decode(ByteBuf buf, Header header, Session session) {
        if (session != null && !StringUtils.hasLength(session.getDevId())) {
            session.register(header.getDevId(), (int) header.getVersion(), header);
        }
        Rs rs = decode0(buf, header, session);
        Rs rsHand = handler(header, session);
        if (rs == null && rsHand != null) {
            rs = rsHand;
        } else if (rs != null && rsHand != null) {
            log.warn("decode0:{} 与 handler:{} 返回值冲突,采用decode0返回值", rs, rsHand);
        }
        if (rs != null) {
            rs.setHeader(header);
        }

        return rs;
    }
}
