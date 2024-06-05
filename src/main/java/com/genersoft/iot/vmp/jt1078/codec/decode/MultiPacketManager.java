package com.genersoft.iot.vmp.jt1078.codec.decode;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public enum MultiPacketManager {
    INSTANCE;
    private final static Logger logger = LoggerFactory.getLogger(MultiPacketManager.class);

    // 用与消息的缓存
    private final Map<String, List<MultiPacket>> packetMap = new ConcurrentHashMap<>();
    private final Map<String, Long> packetTimeMap = new ConcurrentHashMap<>();

    MultiPacketManager() {
        startLister();
    }

    /**
     * 增加待合并的分包，如果分包接受完毕会返回完整的数据包
     */
    public ByteBuf add(MultiPacket packet) {
        String key = packet.getHeader().getMsgId() + "/" + packet.getHeader().getPhoneNumber();
        logger.debug("分包消息： \n{}", packet);
        List<MultiPacket> multiPackets = packetMap.computeIfAbsent(key, k -> new ArrayList<>(packet.getCount()));
        multiPackets.add(packet);
        packetTimeMap.put(key, System.currentTimeMillis());
        if (packet.getCount() == multiPackets.size()) {
            // 所有分包接收完毕，排序后返回
            multiPackets.sort(Comparator.comparing(MultiPacket::getNumber));
            ByteBuf byteBuf = Unpooled.buffer();
            for (MultiPacket multiPacket : multiPackets) {
                byteBuf.writeBytes(multiPacket.getByteBuf());
            }
            packetMap.remove(key);
            packetTimeMap.remove(key);
            return byteBuf;
        }
        return null;
    }

    private void startLister(){
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                long expireTime = System.currentTimeMillis() - 2 * 1000;
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
