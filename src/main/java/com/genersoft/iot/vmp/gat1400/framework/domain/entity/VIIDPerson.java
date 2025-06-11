package com.genersoft.iot.vmp.gat1400.framework.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

import cz.data.viid.framework.domain.dto.SubImageList;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@TableName(value = "viid_persons", autoResultMap = true)
public class VIIDPerson {

    @ApiModelProperty("主键")
    @TableId(value = "id", type = IdType.NONE)
    private String id;
    @ApiModelProperty("数据时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "data_time")
    private LocalDateTime dataTime;
    @ApiModelProperty("人员标识")
    @TableField(value = "person_id")
    private String personId;
    @ApiModelProperty("信息分类")
    @TableField("info_kind")
    private String infoKind;
    @ApiModelProperty("来源标识")
    @TableField("source_id")
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
    @ApiModelProperty("位置标记时间")
    @TableField("location_mark_time")
    private String locationMarkTime;
    @ApiModelProperty("人员出现时间")
    @TableField("person_appear_time")
    private String personAppearTime;
    @ApiModelProperty("人员消失时间")
    @TableField("person_dis_appear_time")
    private String personDisAppearTime;
    @ApiModelProperty("证件种类")
    @TableField(value = "id_type", select = false)
    private String idType;
    @ApiModelProperty("证件号码")
    @TableField(value = "id_number", select = false)
    private String idNumber;
    @ApiModelProperty("姓名")
    @TableField("name")
    private String name;
    @ApiModelProperty("曾用名")
    @TableField("used_name")
    private String usedName;
    @ApiModelProperty("绰号")
    @TableField("alias")
    private String alias;
    @ApiModelProperty("性别代码")
    @TableField("gender_code")
    private String genderCode;
    @ApiModelProperty("年龄上限")
    @TableField("age_up_limit")
    private String ageUpLimit;
    @ApiModelProperty("年龄下限")
    @TableField("age_lower_limit")
    private String ageLowerLimit;
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
    @ApiModelProperty("单位名称")
    @TableField(value = "person_org", select = false)
    private String personOrg;
    @ApiModelProperty("职业类别代码")
    @TableField(value = "job_category", select = false)
    private String jobCategory;
    @ApiModelProperty("同行人数")
    @TableField("accompany_number")
    private String accompanyNumber;
    @ApiModelProperty("身高上限")
    @TableField("height_up_limit")
    private String heightUpLimit;
    @ApiModelProperty("身高下限")
    @TableField("height_lower_limit")
    private String heightLowerLimit;
    @ApiModelProperty("体型")
    @TableField("body_type")
    private String bodyType;
    @ApiModelProperty("肤色")
    @TableField("skin_color")
    private String skinColor;
    @ApiModelProperty("发型")
    @TableField("hair_style")
    private String hairStyle;
    @ApiModelProperty("发色")
    @TableField("hair_color")
    private String hairColor;
    @ApiModelProperty("姿态")
    @TableField("gesture")
    private String gesture;
    @ApiModelProperty("状态")
    @TableField("status")
    private String status;
    @ApiModelProperty("脸型")
    @TableField("face_style")
    private String faceStyle;
    @ApiModelProperty("脸部特征")
    @TableField("facial_feature")
    private String facialFeature;
    @ApiModelProperty("体貌特征")
    @TableField("physical_feature")
    private String physicalFeature;
    @ApiModelProperty("体表特征")
    @TableField("body_feature")
    private String bodyFeature;
    @ApiModelProperty("习惯动作")
    @TableField("habitual_movement")
    private String habitualMovement;
    @ApiModelProperty("行为")
    @TableField("behavior")
    private String behavior;
    @ApiModelProperty("行为描述")
    @TableField("behavior_description")
    private String behaviorDescription;
    @ApiModelProperty("附属物")
    @TableField("appendant")
    private String appendant;
    @ApiModelProperty("附属物描述")
    @TableField("appendant_description")
    private String appendantDescription;
    @ApiModelProperty("伞颜色")
    @TableField("umbrella_color")
    private String umbrellaColor;
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
    @ApiModelProperty("围巾颜色")
    @TableField("scarf_color")
    private String scarfColor;
    @ApiModelProperty("包款式")
    @TableField("bag_style")
    private String bagStyle;
    @ApiModelProperty("包颜色")
    @TableField("bag_color")
    private String bagColor;
    @ApiModelProperty("上衣款式")
    @TableField("coat_style")
    private String coatStyle;
    @ApiModelProperty("上衣长度")
    @TableField("coat_length")
    private String coatLength;
    @ApiModelProperty("上衣颜色")
    @TableField("coat_color")
    private String coatColor;
    @ApiModelProperty("裤子款式")
    @TableField("trousers_style")
    private String trousersStyle;
    @ApiModelProperty("裤子颜色")
    @TableField("trousers_color")
    private String trousersColor;
    @ApiModelProperty("裤子长度")
    @TableField("trousers_len")
    private String trousersLen;
    @ApiModelProperty("鞋子款式")
    @TableField("shoes_style")
    private String shoesStyle;
    @ApiModelProperty("鞋子颜色")
    @TableField("shoes_color")
    private String shoesColor;
    @ApiModelProperty("是否驾驶员")
    @TableField(value = "is_driver", select = false)
    private Integer isDriver;
    @ApiModelProperty("是否涉外人员")
    @TableField(value = "is_foreigner", select = false)
    private Integer isForeigner;
    @ApiModelProperty("护照证件种类")
    @TableField(value = "passport_type", select = false)
    private String passportType;
    @ApiModelProperty("出入境人员类别代码")
    @TableField(value = "immigrant_type_code", select = false)
    private String immigrantTypeCode;
    @ApiModelProperty("是否涉恐人员")
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
    @ApiModelProperty("在押人员身份")
    @TableField(value = "detainees_identity", select = false)
    private String detaineesIdentity;
    @ApiModelProperty("在押特殊人员身份")
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
    @ApiModelProperty("受伤害程度")
    @TableField(value = "injured_degree", select = false)
    private String injuredDegree;
    @ApiModelProperty("尸体状况代码")
    @TableField(value = "corpse_condition_code", select = false)
    private String corpseConditionCode;
    @ApiModelProperty("是否可疑人")
    @TableField(value = "is_suspicious_person", select = false)
    private Integer isSuspiciousPerson;
    @ApiModelProperty("图像列表")
    @TableField(value = "sub_image_list", typeHandler = JacksonTypeHandler.class)
    private SubImageList subImageList;
}
