package com.genersoft.iot.vmp.jt1078.bean;

import com.genersoft.iot.vmp.jt1078.util.BCDUtil;
import com.genersoft.iot.vmp.utils.DateUtil;
import io.netty.buffer.ByteBuf;
import io.swagger.v3.oas.annotations.media.Schema;

import java.nio.charset.Charset;

@Schema(description = "驾驶员身份信息")
public class JTDriverInformation {

    @Schema(description = "0x01:从业资格证 IC卡插入( 驾驶员上班)；0x02:从 业资格证 IC卡拔出(驾驶员下班)")
    private int status;

    @Schema(description = "插卡/拔卡时间 ,以下字段在状 态为0x01 时才有效并做填充")
    private String time;

    @Schema(description = "IC卡读取结果:" +
            "0x00:IC卡读卡成功；" +
            "0x01:读卡失败 ,原因为卡片密钥认证未通过； 0x02:读卡失败 ,原因为卡片已被锁定；" +
            "0x03:读卡失败 ,原因为卡片被拔出；" +
            "0x04:读卡失败 ,原因为数据校验错误。" +
            "以下字段在 IC卡读取结果等于0x00 时才有效")
    private Integer result;

    @Schema(description = "驾驶员姓名")
    private String name;

    @Schema(description = "从业资格证编码")
    private String certificateCode;

    @Schema(description = "发证机构名称")
    private String certificateIssuanceMechanismName;

    @Schema(description = "证件有效期")
    private String expire;

    @Schema(description = "驾驶员身份证号")
    private String driverIdNumber;

    public static JTDriverInformation decode(ByteBuf buf) {
        JTDriverInformation jtDriverInformation = new JTDriverInformation();
        jtDriverInformation.setStatus(buf.readUnsignedByte());
        byte[] bytes = new byte[6];
        buf.readBytes(bytes);
        jtDriverInformation.setTime(DateUtil.jt1078Toyyyy_MM_dd_HH_mm_ss(BCDUtil.transform(bytes)));
        if (jtDriverInformation.getStatus() == 1) {
            jtDriverInformation.setResult((int)buf.readUnsignedByte());
            int nameLength = buf.readUnsignedByte();
            jtDriverInformation.setName(buf.readCharSequence(nameLength, Charset.forName("GBK")).toString().trim());
            jtDriverInformation.setCertificateCode(buf.readCharSequence(20, Charset.forName("GBK")).toString().trim());
            int certificateIssuanceMechanismNameLength = buf.readUnsignedByte();
            jtDriverInformation.setCertificateIssuanceMechanismName(buf.readCharSequence(
                    certificateIssuanceMechanismNameLength, Charset.forName("GBK")).toString().trim());
            byte[] bytesForExpire = new byte[4];
            buf.readBytes(bytesForExpire);
            jtDriverInformation.setExpire(DateUtil.jt1078dateToyyyy_MM_dd(BCDUtil.transform(bytesForExpire)));
            jtDriverInformation.setDriverIdNumber(buf.readCharSequence(20, Charset.forName("GBK")).toString().trim());
        }
        return jtDriverInformation;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public Integer getResult() {
        return result;
    }

    public void setResult(Integer result) {
        this.result = result;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCertificateCode() {
        return certificateCode;
    }

    public void setCertificateCode(String certificateCode) {
        this.certificateCode = certificateCode;
    }

    public String getCertificateIssuanceMechanismName() {
        return certificateIssuanceMechanismName;
    }

    public void setCertificateIssuanceMechanismName(String certificateIssuanceMechanismName) {
        this.certificateIssuanceMechanismName = certificateIssuanceMechanismName;
    }

    public String getExpire() {
        return expire;
    }

    public void setExpire(String expire) {
        this.expire = expire;
    }

    public String getDriverIdNumber() {
        return driverIdNumber;
    }

    public void setDriverIdNumber(String driverIdNumber) {
        this.driverIdNumber = driverIdNumber;
    }

    @Override
    public String toString() {
        return "JTDriverInformation{" +
                "status=" + status +
                ", time='" + time + '\'' +
                ", result=" + result +
                ", name='" + name + '\'' +
                ", certificateCode='" + certificateCode + '\'' +
                ", certificateIssuanceMechanismName='" + certificateIssuanceMechanismName + '\'' +
                ", expire='" + expire + '\'' +
                ", driverIdNumber='" + driverIdNumber + '\'' +
                '}';
    }
}
