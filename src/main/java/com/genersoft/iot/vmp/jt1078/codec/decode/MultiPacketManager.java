package com.genersoft.iot.vmp.jt1078.codec.decode;

import com.genersoft.iot.vmp.jt1078.proc.Header;
import io.netty.buffer.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public enum MultiPacketManager {
    INSTANCE;
    private final static Logger logger = LoggerFactory.getLogger(MultiPacketManager.class);

    // 用与消息的缓存
    private final Map<String, CompositeByteBuf> packetMap = new ConcurrentHashMap<>();
    private final Map<String, Long> packetTimeMap = new ConcurrentHashMap<>();

    MultiPacketManager() {
        startLister();
    }

    /**
     * 增加待合并的分包，如果分包接受完毕会返回完整的数据包
     */
    public ByteBuf add(Header header, Integer count, ByteBuf byteBuf) {
        String key = header.getMsgId() + "/" + header.getPhoneNumber();
        CompositeByteBuf compositeBuf = packetMap.computeIfAbsent(key, k -> new CompositeByteBuf(UnpooledByteBufAllocator.DEFAULT, false, count));
//        compositeBuf.addComponent(true, byteBuf.readSlice(byteBuf.readableBytes()));
        compositeBuf.addComponent(true, byteBuf);
        packetTimeMap.put(key, System.currentTimeMillis());
        if (count == compositeBuf.numComponents()) {
            packetMap.remove(key);
            packetTimeMap.remove(key);
            compositeBuf.retain();
            return compositeBuf;
        }
        return null;
    }

    private void startLister(){
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                long expireTime = System.currentTimeMillis() - 20 * 1000;
                if (!packetTimeMap.isEmpty()) {
                    for (String key : packetTimeMap.keySet()) {
                        if (packetTimeMap.get(key) < expireTime) {
                            logger.info("分包消息超时 key: {}", key);
                            packetTimeMap.remove(key);
                            packetMap.remove(key);
                        }
                    }
                }
            }
        }, 2000L, 2000L);
    }
}
