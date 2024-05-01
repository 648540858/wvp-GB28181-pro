package com.genersoft.iot.vmp.jt1078.proc.response;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import com.genersoft.iot.vmp.jt1078.bean.JTPhoneBookContact;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.nio.charset.Charset;
import java.util.List;

/**
 * 设置电话本
 */
@MsgId(id = "8401")
public class J8401 extends Rs {

    /**
     * 设置类型:
     * 0: 删除终端上所有存储的联系人,
     * 1: 表示更新电话本$ 删除终端中已有全部联系人并追加消 息中的联系人,
     * 2: 表示追加电话本,
     * 3: 表示修改电话本$以联系人为索引
     */
    private int type;

    /**
     * 联系人
     */
    private List<JTPhoneBookContact> phoneBookContactList;

    @Override
    public ByteBuf encode() {
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeByte(type);
        if (phoneBookContactList != null && !phoneBookContactList.isEmpty()) {
            buffer.writeByte(phoneBookContactList.size());
            for (JTPhoneBookContact jtPhoneBookContact : phoneBookContactList) {
                buffer.writeBytes(jtPhoneBookContact.encode());
            }
        }else {
            buffer.writeByte(0);
        }
        return buffer;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public List<JTPhoneBookContact> getPhoneBookContactList() {
        return phoneBookContactList;
    }

    public void setPhoneBookContactList(List<JTPhoneBookContact> phoneBookContactList) {
        this.phoneBookContactList = phoneBookContactList;
    }
}
