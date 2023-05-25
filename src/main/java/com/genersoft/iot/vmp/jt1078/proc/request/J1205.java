package com.genersoft.iot.vmp.jt1078.proc.request;

import com.alibaba.fastjson2.JSON;
import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import com.genersoft.iot.vmp.jt1078.proc.Header;
import com.genersoft.iot.vmp.jt1078.proc.response.J8001;
import com.genersoft.iot.vmp.jt1078.proc.response.Rs;
import com.genersoft.iot.vmp.jt1078.session.Session;
import com.genersoft.iot.vmp.jt1078.session.SessionManager;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * 终端上传音视频资源列表
 *
 * @author QingtaiJiang
 * @date 2023/4/28 10:36
 * @email qingtaij@163.com
 */
@MsgId(id = "1205")
public class J1205 extends Re {
    Integer respNo;

    private List<JRecordItem> recordList = new ArrayList<JRecordItem>();

    @Override
    protected Rs decode0(ByteBuf buf, Header header, Session session) {
        respNo = buf.readUnsignedShort();
        long size = buf.readUnsignedInt();

        for (int i = 0; i < size; i++) {
            JRecordItem item = new JRecordItem();
            item.setChannelId(buf.readUnsignedByte());
            item.setStartTime(ByteBufUtil.hexDump(buf.readSlice(6)));
            item.setEndTime(ByteBufUtil.hexDump(buf.readSlice(6)));
            item.setWarn(buf.readLong());
            item.setMediaType(buf.readUnsignedByte());
            item.setStreamType(buf.readUnsignedByte());
            item.setStorageType(buf.readUnsignedByte());
            item.setSize(buf.readUnsignedInt());
            recordList.add(item);
        }

        return null;
    }

    @Override
    protected Rs handler(Header header, Session session) {
        SessionManager.INSTANCE.response(header.getDevId(), "1205", (long) respNo, JSON.toJSONString(this));

        J8001 j8001 = new J8001();
        j8001.setRespNo(header.getSn());
        j8001.setRespId(header.getMsgId());
        j8001.setResult(J8001.SUCCESS);
        return j8001;
    }


    public Integer getRespNo() {
        return respNo;
    }

    public void setRespNo(Integer respNo) {
        this.respNo = respNo;
    }

    public List<JRecordItem> getRecordList() {
        return recordList;
    }

    public void setRecordList(List<JRecordItem> recordList) {
        this.recordList = recordList;
    }

    public static class JRecordItem {

        // 逻辑通道号
        private int channelId;

        // 开始时间
        private String startTime;

        // 结束时间
        private String endTime;

        // 报警标志
        private long warn;

        // 音视频资源类型
        private int mediaType;

        // 码流类型
        private int streamType = 1;

        // 存储器类型
        private int storageType;

        // 文件大小
        private long size;

        public int getChannelId() {
            return channelId;
        }

        public void setChannelId(int channelId) {
            this.channelId = channelId;
        }

        public String getStartTime() {
            return startTime;
        }

        public void setStartTime(String startTime) {
            this.startTime = startTime;
        }

        public String getEndTime() {
            return endTime;
        }

        public void setEndTime(String endTime) {
            this.endTime = endTime;
        }

        public long getWarn() {
            return warn;
        }

        public void setWarn(long warn) {
            this.warn = warn;
        }

        public int getMediaType() {
            return mediaType;
        }

        public void setMediaType(int mediaType) {
            this.mediaType = mediaType;
        }

        public int getStreamType() {
            return streamType;
        }

        public void setStreamType(int streamType) {
            this.streamType = streamType;
        }

        public int getStorageType() {
            return storageType;
        }

        public void setStorageType(int storageType) {
            this.storageType = storageType;
        }

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }

        @Override
        public String toString() {
            return "JRecordItem{" +
                    "channelId=" + channelId +
                    ", startTime='" + startTime + '\'' +
                    ", endTime='" + endTime + '\'' +
                    ", warn=" + warn +
                    ", mediaType=" + mediaType +
                    ", streamType=" + streamType +
                    ", storageType=" + storageType +
                    ", size=" + size +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "J1205{" +
                "respNo=" + respNo +
                ", recordList=" + recordList +
                '}';
    }
}
