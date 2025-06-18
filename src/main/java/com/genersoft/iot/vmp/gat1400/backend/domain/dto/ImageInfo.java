package com.genersoft.iot.vmp.gat1400.backend.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class ImageInfo {

    @ApiModelProperty("图片ID")
    @JsonProperty("ImageID")
    private String imageId;
    @ApiModelProperty("信息分类")
    @JsonProperty("InfoKind")
    private Integer infoKind;
    @ApiModelProperty("图像来源")
    @JsonProperty("ImageSource")
    private String imageSource;
    @ApiModelProperty(value = "来源视频标识", notes = "如果此图像是视频截图,此字段是来源视频的视频ID")
    @JsonProperty("SourceVideoID")
    private String sourceVideoId;
    @ApiModelProperty(value = "原始图像标识", notes = "图像增强处理输出图像对应的原始图像ID,增强处理后图像必选")
    @JsonProperty("OriginImageID")
    private String originImageId;
    @ApiModelProperty("事件分类")
    @JsonProperty("EventSort")
    private Integer eventSort;
    @ApiModelProperty("设备编码")
    @JsonProperty("DeviceID")
    private String deviceId;
    @ApiModelProperty("存储路径")
    @JsonProperty("StoragePath")
    private String storagePath;
    @ApiModelProperty(value = "图像文件哈希值", notes = "O 使用MD5算法")
    @JsonProperty("FileHash")
    private String fileHash;
    @ApiModelProperty(value = "图片格式", notes = "R (Jpeg)")
    @JsonProperty("FileFormat")
    private String fileFormat;
    @ApiModelProperty(value = "拍摄时间", notes = "R")
    @JsonFormat(pattern = "yyyyMMddHHmmss")
    @JsonProperty("ShotTime")
    private LocalDateTime shotTime;
    @ApiModelProperty(value = "题名", notes = "R 图像资料名称的汉语描述")
    @JsonProperty("Title")
    private String title;
    @ApiModelProperty(value = "题名补充", notes = "O 题名的补充和备注信息")
    @JsonProperty("TitleNote")
    private String titleNote;
    @ApiModelProperty(value = "专项名", notes = "O 图像资料所属的专项名称")
    @JsonProperty("SpecialIName")
    private String specialIName;
    @ApiModelProperty(value = "关键词", notes = "O 能够正确表述图像资料主要内容,具有检索意义的词或词组")
    @JsonProperty("Keyword")
    private String keyword;
    @ApiModelProperty(value = "内容描述", notes = "O 对图像内容的简要描述")
    @JsonProperty("ContentDescription")
    private String contentDescription;
    @ApiModelProperty(value = "主题人物", notes = "O 图像资料内出现的主要人物的中文姓名全称,当有多个时用英文半角分号”;”分隔")
    @JsonProperty("SubjectCharacter")
    private String subjectCharacter;
    @ApiModelProperty(value = "拍摄地点行政区划代码", notes = "R/O 人工采集图像才需要")
    @JsonProperty("ShotPlaceCode")
    private String shotPlaceCode;
    @ApiModelProperty(value = "拍摄地点区划内详细地址", notes = "具体到街道门牌号")
    @JsonProperty("ShotPlaceFullAdress")
    private String shotPlaceFullAdress;
    @ApiModelProperty(value = "拍摄地点经度")
    @JsonProperty("ShotPlaceLongitude")
    private String shotPlaceLongitude;
    @ApiModelProperty(value = "拍摄地点纬度")
    @JsonProperty("ShotPlaceLatitude")
    private String shotPlaceLatitude;
    @ApiModelProperty(value = "密级代码", notes = "R 自动采集时取值为5")
    @JsonProperty("SecurityLevel")
    private String securityLevel;
    @ApiModelProperty("图片宽度")
    @JsonProperty("Width")
    private String width;
    @ApiModelProperty("图片高度")
    @JsonProperty("Height")
    private String height;
    @ApiModelProperty(value = "采集人", notes = "R/O 图像资料的采集人姓名或采集系统名称,人工采集必选")
    @JsonProperty("CollectorName")
    private String collectorName;
    @ApiModelProperty(value = "采集单位名称", notes = "R/O 图像资料的采集单位名称,人工采集必选")
    @JsonProperty("CollectorOrg")
    private String collectorOrg;
    @ApiModelProperty(value = "入库人", notes = "R/O 图像资料的入库人姓名或入库系统名称,人工采集必选")
    @JsonProperty("EntryClerk")
    private String entryClerk;
    @ApiModelProperty(value = "入库单位名称", notes = "R/O 图像资料的入库单位名称，人工采集必选")
    @JsonProperty("EntryClerkOrg")
    private String entryClerkOrg;
    @ApiModelProperty(value = "入库时间", notes = "R 视图库自动生成,创建报文中不需要该字段")
    @JsonFormat(pattern = "yyyyMMddHHmmss")
    @JsonProperty("EntryTime")
    private LocalDateTime entryTime;
    @ApiModelProperty(value = "文件大小", notes = "R/O 图像文件大小，单位byte")
    @JsonProperty("FileSize")
    private Long fileSize;
}
