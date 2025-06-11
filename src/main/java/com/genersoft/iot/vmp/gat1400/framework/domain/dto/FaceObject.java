package com.genersoft.iot.vmp.gat1400.framework.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.apache.commons.lang3.StringUtils;

import cz.data.viid.utils.DateUtil;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class FaceObject {

    @ApiModelProperty("人脸标识")
    @JsonProperty("FaceID")
    private String FaceID;
    @ApiModelProperty("信息分类")
    @JsonProperty("InfoKind")
    private Integer InfoKind;
    @ApiModelProperty("来源标识")
    @JsonProperty("SourceID")
    private String SourceID;
    @ApiModelProperty("设备编码")
    @JsonProperty("DeviceID")
    private String DeviceID;
    @ApiModelProperty("左上角X坐标")
    @JsonProperty("LeftTopX")
    private Integer LeftTopX;
    @ApiModelProperty("左上角Y坐标")
    @JsonProperty("LeftTopY")
    private Integer LeftTopY;
    @ApiModelProperty("右下角X坐标")
    @JsonProperty("RightBtmX")
    private Integer RightBtmX;
    @ApiModelProperty("右下角Y坐标")
    @JsonProperty("RightBtmY")
    private Integer RightBtmY;
    @ApiModelProperty("位置标记时间")
    @JsonProperty("LocationMarkTime")
    private String LocationMarkTime;
    @ApiModelProperty("人脸出现时间")
    @JsonProperty("FaceAppearTime")
    private String FaceAppearTime;
    @ApiModelProperty("人脸消失时间")
    @JsonProperty("FaceDisAppearTime")
    private String FaceDisAppearTime;
    @ApiModelProperty("镜头时间")
    @JsonProperty("ShotTime")
    private String ShotTime;
    @ApiModelProperty("证件种类")
    @JsonProperty("IDType")
    private String IDType;
    @ApiModelProperty("证件号码")
    @JsonProperty("IDNumber")
    private String IDNumber;
    @ApiModelProperty("姓名")
    @JsonProperty("Name")
    private String Name;
    @ApiModelProperty("曾用名")
    @JsonProperty("UsedName")
    private String UsedName;
    @ApiModelProperty("绰号")
    @JsonProperty("Alias")
    private String Alias;
    @ApiModelProperty("性别代码")
    @JsonProperty("GenderCode")
    private String GenderCode;
    @ApiModelProperty("年龄上限")
    @JsonProperty("AgeUpLimit")
    private Integer AgeUpLimit;
    @ApiModelProperty("年龄下限")
    @JsonProperty("AgeLowerLimit")
    private Integer AgeLowerLimit;
    @ApiModelProperty("民族代码")
    @JsonProperty("EthicCode")
    private String EthicCode;
    @ApiModelProperty("国籍代码")
    @JsonProperty("NationalityCode")
    private String NationalityCode;
    @ApiModelProperty("籍贯省市县代码")
    @JsonProperty("NativeCityCode")
    private String NativeCityCode;
    @ApiModelProperty("住地行政区划")
    @JsonProperty("ResidenceAdminDivision")
    private String ResidenceAdminDivision;
    @ApiModelProperty("汉语口音代码")
    @JsonProperty("ChineseAccentCode")
    private String ChineseAccentCode;
    @ApiModelProperty("职业类别代码")
    @JsonProperty("JobCategory")
    private String JobCategory;
    @ApiModelProperty("同行人脸数")
    @JsonProperty("AccompanyNumber")
    private Integer AccompanyNumber;
    @ApiModelProperty("肤色")
    @JsonProperty("SkinColor")
    private String SkinColor;
    @ApiModelProperty("发型")
    @JsonProperty("HairStyle")
    private String HairStyle;
    @ApiModelProperty("发色")
    @JsonProperty("HairColor")
    private String HairColor;
    @ApiModelProperty("脸型")
    @JsonProperty("FaceStyle")
    private String FaceStyle;
    @ApiModelProperty("脸部特征")
    @JsonProperty("FacialFeature")
    private String FacialFeature;
    @ApiModelProperty("体貌特征")
    @JsonProperty("PhysicalFeature")
    private String PhysicalFeature;
    @ApiModelProperty("口罩颜色")
    @JsonProperty("RespiratorColor")
    private String RespiratorColor;
    @ApiModelProperty("帽子款式")
    @JsonProperty("CapStyle")
    private String CapStyle;
    @ApiModelProperty("帽子颜色")
    @JsonProperty("CapColor")
    private String CapColor;
    @ApiModelProperty("眼镜款式")
    @JsonProperty("GlassStyle")
    private String GlassStyle;
    @ApiModelProperty("眼镜颜色")
    @JsonProperty("GlassColor")
    private String GlassColor;
    @ApiModelProperty("是否驾驶员")
    @JsonProperty("IsDriver")
    private Integer IsDriver;
    @ApiModelProperty("是否涉外人员")
    @JsonProperty("IsForeigner")
    private Integer IsForeigner;
    @ApiModelProperty("护照证件种类")
    @JsonProperty("PassportType")
    private String PassportType;
    @ApiModelProperty("出入境人员类别代码")
    @JsonProperty("ImmigrantTypeCode")
    private String ImmigrantTypeCode;
    @ApiModelProperty("是否涉恐人员")
    @JsonProperty("IsSuspectedTerrorist")
    private Integer IsSuspectedTerrorist;
    @ApiModelProperty("涉恐人员编号")
    @JsonProperty("SuspectedTerroristNumber")
    private String SuspectedTerroristNumber;
    @ApiModelProperty("是否涉案人员")
    @JsonProperty("IsCriminalInvolved")
    private Integer IsCriminalInvolved;
    @ApiModelProperty("涉案人员专长代码")
    @JsonProperty("CriminalInvolvedSpecilisationCode")
    private String CriminalInvolvedSpecilisationCode;
    @ApiModelProperty("体表特殊标记")
    @JsonProperty("BodySpeciallMark")
    private String BodySpeciallMark;
    @ApiModelProperty("作案手段")
    @JsonProperty("CrimeMethod")
    private String CrimeMethod;
    @ApiModelProperty("作案特点代码")
    @JsonProperty("CrimeCharacterCode")
    private String CrimeCharacterCode;
    @ApiModelProperty("在逃人员编号")
    @JsonProperty("EscapedCriminalNumber")
    private String EscapedCriminalNumber;
    @ApiModelProperty("是否在押人员")
    @JsonProperty("IsDetainees")
    private Integer IsDetainees;
    @ApiModelProperty("看守所编码")
    @JsonProperty("DetentionHouseCode")
    private String DetentionHouseCode;
    @ApiModelProperty("在押人员身份")
    @JsonProperty("DetaineesIdentity")
    private String DetaineesIdentity;
    @ApiModelProperty("在押人员特殊身份")
    @JsonProperty("DetaineesSpecialIdentity")
    private String DetaineesSpecialIdentity;
    @ApiModelProperty("成员类型代码")
    @JsonProperty("MemberTypeCode")
    private String MemberTypeCode;
    @ApiModelProperty("是否被害人")
    @JsonProperty("IsVictim")
    private Integer IsVictim;
    @ApiModelProperty("被害人种类")
    @JsonProperty("VictimType")
    private String VictimType;
    @ApiModelProperty("受伤害程度")
    @JsonProperty("InjuredDegree")
    private String InjuredDegree;
    @ApiModelProperty("尸体状况代码")
    @JsonProperty("CorpseConditionCode")
    private String CorpseConditionCode;
    @ApiModelProperty("是否可疑人")
    @JsonProperty("IsSuspiciousPerson")
    private Integer IsSuspiciousPerson;
    @ApiModelProperty("姿态分布")
    @JsonProperty("Attitude")
    private Integer Attitude;
    @ApiModelProperty("相似度")
    @JsonProperty("Similaritydegree")
    private String Similaritydegree;
    @ApiModelProperty("眉型")
    @JsonProperty("EyebrowStyle")
    private String EyebrowStyle;
    @ApiModelProperty("鼻型")
    @JsonProperty("NoseStyle")
    private String NoseStyle;
    @ApiModelProperty("胡型")
    @JsonProperty("MustacheStyle")
    private String MustacheStyle;
    @ApiModelProperty("嘴唇")
    @JsonProperty("LipStyle")
    private String LipStyle;
    @ApiModelProperty("皱纹眼袋")
    @JsonProperty("WrinklePouch")
    private String WrinklePouch;
    @ApiModelProperty("痤疮色斑")
    @JsonProperty("AcneStain")
    private String AcneStain;
    @ApiModelProperty("黑痣胎记")
    @JsonProperty("FreckleBirthmark")
    private String FreckleBirthmark;
    @ApiModelProperty("疤痕酒窝")
    @JsonProperty("ScarDimple")
    private String ScarDimple;
    @ApiModelProperty("其他特征")
    @JsonProperty("OtherFeature")
    private String OtherFeature;
    @ApiModelProperty("图片列表")
    @JsonProperty("SubImageList")
    private SubImageList SubImageList;
    //非国标海康平台字段
    @ApiModelProperty("-")
    @JsonProperty("Maritalstatus")
    private String Maritalstatus;
    @ApiModelProperty("-")
    @JsonProperty("TabID")
    private String TabID;
    @ApiModelProperty("-")
    @JsonProperty("FamilyAddress")
    private String FamilyAddress;
    @ApiModelProperty("-")
    @JsonProperty("CollectorOrg")
    private String CollectorOrg;
    @ApiModelProperty("-")
    @JsonProperty("CollectorID")
    private String CollectorID;
    @ApiModelProperty("设备snno")
    @JsonProperty("DeviceSNNo")
    private String DeviceSNNo;
    @ApiModelProperty("-")
    @JsonProperty("APSId")
    private String APSId;
    @ApiModelProperty("通过时间")
    @JsonProperty("EntryTime")
    private String EntryTime;

    public FaceObject validateDataFormat() {
        String id = getFaceID();
        String deviceID = getDeviceID();
        if (StringUtils.isNotBlank(id)
                && id.length() > deviceID.length()
                && !StringUtils.startsWith(id, deviceID)) {
            String subId = StringUtils.substring(id, deviceID.length());
            setFaceID(deviceID + subId);
        }
        if (StringUtils.isBlank(getFaceAppearTime())) {
            String dateTime = DateUtil.extractIdDateTime(getFaceID());
            if (dateTime != null) {
                setFaceAppearTime(dateTime);
                setFaceDisAppearTime(dateTime);
            }
        }
        return this;
    }
}
