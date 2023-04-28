package com.genersoft.iot.vmp.jt1078.proc.request;

import com.genersoft.iot.vmp.jt1078.proc.Header;
import com.genersoft.iot.vmp.jt1078.proc.response.Rs;
import com.genersoft.iot.vmp.jt1078.session.Session;
import io.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

/**
 * @author QingtaiJiang
 * @date 2023/4/27 18:50
 * @email qingtaij@163.com
 */
public abstract class Re {
    private final static Logger log = LoggerFactory.getLogger(Re.class);

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
