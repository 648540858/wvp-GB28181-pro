package com.genersoft.iot.vmp.jt1078.session;

import com.genersoft.iot.vmp.jt1078.proc.Header;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author QingtaiJiang
 * @date 2023/4/27 18:54
 * @email qingtaij@163.com
 */
public class Session {
    private final static Logger log = LoggerFactory.getLogger(Session.class);

    public static final AttributeKey<Session> KEY = AttributeKey.newInstance(Session.class.getName());

    // Netty的channel
    protected final Channel channel;

    // 原子类的自增ID
    private final AtomicInteger serialNo = new AtomicInteger(0);

    // 是否注册成功
    private boolean registered = false;

    // 设备ID
    private String devId;

    // 创建时间
    private final long creationTime;

    // 协议版本号
    private Integer protocolVersion;

    private Header header;

    protected Session(Channel channel) {
        this.channel = channel;
        this.creationTime = System.currentTimeMillis();
    }

    public void writeObject(Object message) {
        log.info("<<<<<<<<<< cmd{},{}", this, message);
        channel.writeAndFlush(message);
    }

    /**
     * 获得下一个流水号
     *
     * @return 流水号
     */
    public int nextSerialNo() {
        int current;
        int next;
        do {
            current = serialNo.get();
            next = current > 0xffff ? 0 : current;
        } while (!serialNo.compareAndSet(current, next + 1));
        return next;
    }

    /**
     * 注册session
     *
     * @param devId 设备ID
     */
    public void register(String devId, Integer version, Header header) {
        this.devId = devId;
        this.registered = true;
        this.protocolVersion = version;
        this.header = header;
        SessionManager.INSTANCE.put(devId, this);
    }

    /**
     * 获取设备号
     *
     * @return 设备号
     */
    public String getDevId() {
        return devId;
    }


    public boolean isRegistered() {
        return registered;
    }

    public long getCreationTime() {
        return creationTime;
    }

    public Integer getProtocolVersion() {
        return protocolVersion;
    }

    public Header getHeader() {
        return header;
    }

    @Override
    public String toString() {
        return "[" +
                "devId=" + devId +
                ", reg=" + registered +
                ", version=" + protocolVersion +
                ",ip=" + channel.remoteAddress() +
                ']';
    }
}
