package com.genersoft.iot.vmp.jt1078.proc.request;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import com.genersoft.iot.vmp.jt1078.bean.JTDevice;
import com.genersoft.iot.vmp.jt1078.proc.Header;
import com.genersoft.iot.vmp.jt1078.proc.response.J8001;
import com.genersoft.iot.vmp.jt1078.proc.response.Rs;
import com.genersoft.iot.vmp.jt1078.service.Ijt1078Service;
import com.genersoft.iot.vmp.jt1078.session.Session;
import io.netty.buffer.ByteBuf;
import io.netty.util.CharsetUtil;
import org.springframework.context.ApplicationEvent;

import java.nio.charset.Charset;

/**
 * 终端鉴权
 *
 * @author QingtaiJiang
 * @date 2023/4/27 18:06
 * @email qingtaij@163.com
 */
@MsgId(id = "0102")
public class J0102 extends Re {

    private String authenticationCode;

    @Override
    protected Rs decode0(ByteBuf buf, Header header, Session session) {
        int lenCode = buf.readUnsignedByte();
        byte[] authenticationCodeBytes = new byte[lenCode];
//        ByteBuf byteBuf = buf.readBytes(authenticationCodeBytes);
        authenticationCode = buf.readCharSequence(lenCode, Charset.forName("GBK")).toString();
        System.out.println("设备鉴权： authenticationCode： " + authenticationCode);
        // if 2019 to decode next
        return null;
    }

    @Override
    protected Rs handler(Header header, Session session, Ijt1078Service service) {
        JTDevice device = service.getDevice(header.getTerminalId());
        J8001 j8001 = new J8001();
        j8001.setRespNo(header.getSn());
        j8001.setRespId(header.getMsgId());
        if (device == null || !device.getAuthenticationCode().equals(authenticationCode)) {
            j8001.setResult(J8001.FAIL);
        }else {
            j8001.setResult(J8001.SUCCESS);
        }
        return j8001;
    }

    @Override
    public ApplicationEvent getEvent() {
        return null;
    }

}
