package com.genersoft.iot.vmp.jt1078.bean;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.swagger.v3.oas.annotations.media.Schema;

import java.nio.charset.Charset;

@Schema(description = "电话本联系人")
public class JTPhoneBookContact {

    @Schema(description = "1:呼入,2:呼出,3:呼入/呼出")
    private int sign;

    @Schema(description = "电话号码")
    private String phoneNumber;

    @Schema(description = "联系人")
    private String contactName;

    public ByteBuf encode(){
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeByte(sign);
        buffer.writeByte(phoneNumber.getBytes(Charset.forName("GBK")).length);
        buffer.writeCharSequence(phoneNumber, Charset.forName("GBK"));
        buffer.writeByte(contactName.getBytes(Charset.forName("GBK")).length);
        buffer.writeCharSequence(contactName, Charset.forName("GBK"));
        return buffer;
    }

    public int getSign() {
        return sign;
    }

    public void setSign(int sign) {
        this.sign = sign;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    @Override
    public String toString() {
        return "JTPhoneBookContact{" +
                "sign=" + sign +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", contactName='" + contactName + '\'' +
                '}';
    }
}
