package com.genersoft.iot.vmp.gb28181.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 国标编码对象
 */
@Data
@Schema(description = "国标编码对象")
public class GbCode {

    @Schema(description = "中心编码,由监控中心所在地的行政区划代码确定,符合GB/T2260—2007的要求")
    private String centerCode;

    @Schema(description = "行业编码")
    private String industryCode;

    @Schema(description = "类型编码")
    private String typeCode;

    @Schema(description = "网络标识")
    private String netCode;

    @Schema(description = "序号")
    private String sn;

    /**
     * 解析国标编号
     */
    public static GbCode decode(String code){
        if (code == null || code.trim().length() != 20) {
            return null;
        }
        code = code.trim();
        GbCode gbCode = new GbCode();
        gbCode.setCenterCode(code.substring(0, 8));
        gbCode.setIndustryCode(code.substring(9, 10));
        gbCode.setTypeCode(code.substring(11, 13));
        gbCode.setNetCode(code.substring(14, 15));
        gbCode.setSn(code.substring(15, 20));
        return gbCode;
    }

    public String ecode(){
        return centerCode + industryCode + typeCode + netCode + sn;
    }
}
