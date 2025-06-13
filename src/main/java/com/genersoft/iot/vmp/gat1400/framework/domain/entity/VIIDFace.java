package com.genersoft.iot.vmp.gat1400.framework.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.genersoft.iot.vmp.gat1400.framework.domain.dto.SubImageList;

import java.time.LocalDateTime;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@TableName(value = "viid_faces", autoResultMap = true)
public class VIIDFace {

    @ApiModelProperty("主键")
    @TableId(value = "id", type = IdType.NONE)
    private String id;
    @ApiModelProperty("数据时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "data_time")
    private LocalDateTime dataTime;
    @ApiModelProperty("人脸标识")
    @TableField(value = "face_id")
    private String faceId;
    @ApiModelProperty("信息分类")
    @TableField(value = "info_kind", select = false)
    private Integer infoKind;
    @ApiModelProperty("来源标识")
    @TableField(value = "source_id", select = false)
    private String sourceId;
    @ApiModelProperty("设备编码")
    @TableField("device_id")
    private String deviceId;
    @ApiModelProperty("左上角X坐标")
    @TableField("left_top_x")
    private Integer leftTopX;
    @ApiModelProperty("左上角Y坐标")
    @TableField("left_top_y")
    private Integer leftTopY;
    @ApiModelProperty("右下角X坐标")
    @TableField("right_btm_x")
    private Integer rightBtmX;
    @ApiModelProperty("右下角Y坐标")
    @TableField("right_btm_y")
    private Integer rightBtmY;
    @ApiModelProperty("证件种类")
    @TableField("id_type")
    private String idType;
    @ApiModelProperty("证件号码")
    @TableField("id_number")
    private String idNumber;
    @ApiModelProperty("姓名")
    @TableField("name")
    private String name;
    @ApiModelProperty("曾用名")
    @TableField(value = "used_name", select = false)
    private String usedName;
    @ApiModelProperty("绰号")
    @TableField(value = "`alias`", select = false)
    private String alias;
    @ApiModelProperty("性别代码")
    @TableField("gender_code")
    private String genderCode;
    @ApiModelProperty("年龄上限")
    @TableField("age_up_limit")
    private Integer ageUpLimit;
    @ApiModelProperty("年龄下限")
    @TableField("age_lower_limit")
    private Integer ageLowerLimit;
    @ApiModelProperty("民族代码")
    @TableField(value = "ethic_code", select = false)
    private String ethicCode;
    @ApiModelProperty("国籍代码")
    @TableField(value = "nationality_code", select = false)
    private String nationalityCode;
    @ApiModelProperty("籍贯省市县代码")
    @TableField(value = "native_city_code", select = false)
    private String nativeCityCode;
    @ApiModelProperty("居住地行政区划")
    @TableField(value = "residence_admin_division", select = false)
    private String residenceAdminDivision;
    @ApiModelProperty("汉语口音代码")
    @TableField(value = "chinese_accent_code", select = false)
    private String chineseAccentCode;
    @ApiModelProperty("职业类别代码")
    @TableField(value = "job_category", select = false)
    private String jobCategory;
    @ApiModelProperty("同行人脸数")
    @TableField("accompany_number")
    private Integer accompanyNumber;
    @ApiModelProperty("肤色")
    @TableField("skin_color")
    private String skinColor;
    @ApiModelProperty("脸型")
    @TableField("face_style")
    private String faceStyle;
    @ApiModelProperty("脸部特征")
    @TableField("facial_feature")
    private String facialFeature;
    @ApiModelProperty("体貌特征")
    @TableField("physical_feature")
    private String physicalFeature;
    @ApiModelProperty("是否驾驶员")
    @TableField(value = "is_driver", select = false)
    private Integer isDriver;
    @ApiModelProperty("是否涉外人员")
    @TableField(value = "is_foreigner", select = false)
    private Integer isForeigner;
    @ApiModelProperty("出入境人员类别代码")
    @TableField(value = "immigrant_type_code", select = false)
    private String immigrantTypeCode;
    @ApiModelProperty(value = "是否涉恐人员")
    @TableField(value = "is_suspected_terrorist", select = false)
    private Integer isSuspectedTerrorist;
    @ApiModelProperty("涉恐人员编号")
    @TableField(value = "suspected_terrorist_number", select = false)
    private String suspectedTerroristNumber;
    @ApiModelProperty("是否涉案人员")
    @TableField(value = "is_criminal_involved", select = false)
    private Integer isCriminalInvolved;
    @ApiModelProperty("涉案人员专长代码")
    @TableField(value = "criminal_involved_specilisation_code", select = false)
    private String criminalInvolvedSpecilisationCode;
    @ApiModelProperty("体表特殊标记")
    @TableField("body_speciall_mark")
    private String bodySpeciallMark;
    @ApiModelProperty("作案手段")
    @TableField(value = "crime_method", select = false)
    private String crimeMethod;
    @ApiModelProperty("作案特点代码")
    @TableField(value = "crime_character_code", select = false)
    private String crimeCharacterCode;
    @ApiModelProperty("在逃人员编号")
    @TableField(value = "escaped_criminal_number", select = false)
    private String escapedCriminalNumber;
    @ApiModelProperty("是否在押人员")
    @TableField(value = "is_detainees", select = false)
    private Integer isDetainees;
    @ApiModelProperty("看守所编码")
    @TableField(value = "detention_house_code", select = false)
    private String detentionHouseCode;
    @ApiModelProperty("在押人员特殊身份")
    @TableField(value = "detainees_special_identity", select = false)
    private String detaineesSpecialIdentity;
    @ApiModelProperty("成员类型代码")
    @TableField(value = "member_type_code", select = false)
    private String memberTypeCode;
    @ApiModelProperty("是否被害人")
    @TableField(value = "is_victim", select = false)
    private Integer isVictim;
    @ApiModelProperty("被害人种类")
    @TableField(value = "victim_type", select = false)
    private String victimType;
    @ApiModelProperty("尸体状况代码")
    @TableField(value = "corpse_condition_code", select = false)
    private String corpseConditionCode;
    @ApiModelProperty("是否可疑人")
    @TableField(value = "is_suspicious_person", select = false)
    private Integer isSuspiciousPerson;
    @ApiModelProperty("姿态分布")
    @TableField("attitude")
    private Integer attitude;
    @ApiModelProperty("相似度")
    @TableField("similaritydegree")
    private String similaritydegree;
    @ApiModelProperty("眉型")
    @TableField("eyebrow_style")
    private String eyebrowStyle;
    @ApiModelProperty("鼻型")
    @TableField("nose_style")
    private String noseStyle;
    @ApiModelProperty("胡型")
    @TableField("mustache_style")
    private String mustacheStyle;
    @ApiModelProperty("嘴唇")
    @TableField("lip_style")
    private String lipStyle;
    @ApiModelProperty("皱纹眼袋")
    @TableField("wrinkle_pouch")
    private String wrinklePouch;
    @ApiModelProperty("痤疮色斑")
    @TableField("acne_stain")
    private String acneStain;
    @ApiModelProperty("黑痣胎记")
    @TableField("freckle_birthmark")
    private String freckleBirthmark;
    @ApiModelProperty("疤痕酒窝")
    @TableField("scar_dimple")
    private String scarDimple;
    @ApiModelProperty("-")
    @TableField(value = "tab_id", exist = false)
    private String tabId;
    @ApiModelProperty("其他特征")
    @TableField("other_feature")
    private String otherFeature;
    @ApiModelProperty("婚姻状况")
    @TableField(value = "maritalstatus", select = false)
    private String maritalstatus;
    @ApiModelProperty("家庭地址")
    @TableField(value = "family_address", select = false)
    private String familyAddress;
    @ApiModelProperty("-")
    @TableField(value = "collector_org", exist = false)
    private String collectorOrg;
    @ApiModelProperty("-")
    @TableField(value = "collector_id", exist = false)
    private String collectorId;
    @ApiModelProperty("设备snno")
    @TableField(value = "device_snno", exist = false)
    private String deviceSnno;
    @ApiModelProperty("-")
    @TableField(value = "aps_id", exist = false)
    private String apsId;
    @ApiModelProperty("位置标记时间")
    @TableField("location_mark_time")
    private String locationMarkTime;
    @ApiModelProperty("人脸出现时间")
    @TableField("face_appear_time")
    private String faceAppearTime;
    @ApiModelProperty("人脸消失时间")
    @TableField("face_dis_appear_time")
    private String faceDisAppearTime;
    @ApiModelProperty("镜头时间")
    @TableField("shot_time")
    private String shotTime;
    @ApiModelProperty("发型")
    @TableField("hair_style")
    private String hairStyle;
    @ApiModelProperty("发色")
    @TableField("hair_color")
    private String hairColor;
    @ApiModelProperty("口罩颜色")
    @TableField("respirator_color")
    private String respiratorColor;
    @ApiModelProperty("帽子款式")
    @TableField("cap_style")
    private String capStyle;
    @ApiModelProperty("帽子颜色")
    @TableField("cap_color")
    private String capColor;
    @ApiModelProperty("眼镜款式")
    @TableField("glass_style")
    private String glassStyle;
    @ApiModelProperty("眼镜颜色")
    @TableField("glass_color")
    private String glassColor;
    @ApiModelProperty("护照证件种类")
    @TableField(value = "passport_type", select = false)
    private String passportType;
    @ApiModelProperty("在押人员身份")
    @TableField(value = "detainees_identity", select = false)
    private String detaineesIdentity;
    @ApiModelProperty("受伤害程度")
    @TableField(value = "injured_degree", select = false)
    private String injuredDegree;
    @ApiModelProperty("通过时间")
    @TableField(value = "entry_time", exist = false)
    private String entryTime;
    @ApiModelProperty("图片列表")
    @TableField(value = "sub_image_list", typeHandler = JacksonTypeHandler.class)
    private SubImageList subImageList;
}
