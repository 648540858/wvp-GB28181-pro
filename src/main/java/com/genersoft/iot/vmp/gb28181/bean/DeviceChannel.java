package com.genersoft.iot.vmp.gb28181.bean;

import com.genersoft.iot.vmp.gb28181.utils.MessageElement;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Schema(description = "通道信息")
@EqualsAndHashCode(callSuper = true)
public class DeviceChannel extends CommonGBChannel {

	@MessageElement("DeviceID")
	@Schema(description = "编码")
	private String deviceId;

	@MessageElement("DeviceID")
	@Schema(description = "名称")
	private String name;

	@MessageElement("DeviceID")
	@Schema(description = "设备厂商")
	private String manufacturer;

	@MessageElement("DeviceID")
	@Schema(description = "设备型号")
	private String model;

	// 2016
	@MessageElement("DeviceID")
	@Schema(description = "设备归属")
	private String owner;

	@MessageElement("DeviceID")
	@Schema(description = "行政区域")
	private String civilCode;

	@MessageElement("DeviceID")
	@Schema(description = "警区")
	private String block;

	@MessageElement("DeviceID")
	@Schema(description = "安装地址")
	private String address;

	@MessageElement("DeviceID")
	@Schema(description = "是否有子设备")
	private Boolean parental;


	@MessageElement("DeviceID")
	@Schema(description = "父节点ID")
	private String parentId;

	// 2016
	@MessageElement("DeviceID")
	@Schema(description = "信令安全模式")
	private Integer safetyWay;

	@MessageElement("DeviceID")
	@Schema(description = "注册方式")
	private Integer registerWay;

	// 2016
	@MessageElement("DeviceID")
	@Schema(description = "证书序列号")
	private Integer certNum;

	// 2016
	@MessageElement("DeviceID")
	@Schema(description = "证书有效标识")
	private Integer certifiable;

	// 2016
	@MessageElement("DeviceID")
	@Schema(description = "无效原因码(有证书且证书无效的设备必选)")
	private Integer errCode;

	// 2016
	@MessageElement("DeviceID")
	@Schema(description = "证书终止有效期(有证书且证书无效的设备必选)")
	private Integer endTime;

	// 2022
	@MessageElement("DeviceID")
	@Schema(description = "摄像机安全能力等级代码")
	private String securityLevelCode;

	@MessageElement("DeviceID")
	@Schema(description = "保密属性(必选)缺省为0;0-不涉密,1-涉密")
	private Integer secrecy;

	@MessageElement("DeviceID")
	@Schema(description = "设备/系统IPv4/IPv6地址")
	private String ipAddress;

	@MessageElement("DeviceID")
	@Schema(description = "设备/系统端口")
	private Integer port;

	@MessageElement("DeviceID")
	@Schema(description = "设备口令")
	private String password;

	@MessageElement("DeviceID")
	@Schema(description = "设备状态")
	private Boolean status;

	@MessageElement("DeviceID")
	@Schema(description = "经度 WGS-84坐标系")
	private Double longitude;


	@MessageElement("DeviceID")
	@Schema(description = ",纬度 WGS-84坐标系")
	private Double latitude;

	@MessageElement("DeviceID")
	@Schema(description = "虚拟组织所属的业务分组ID")
	private String businessGroupId;

	@MessageElement("DeviceID")
	@Schema(description = "摄像机结构类型,标识摄像机类型: 1-球机; 2-半球; 3-固定枪机; 4-遥控枪机;5-遥控半球;6-多目设备的全景/拼接通道;7-多目设备的分割通道")
	private Integer ptzType;

	@MessageElement("DeviceID")
	@Schema(description = "摄像机光电成像类型。1-可见光成像;2-热成像;3-雷达成像;4-X光成像;5-深度光场成像;9-其他。可多值,")
	private String photoelectricImagingTyp;

	@MessageElement("DeviceID")
	@Schema(description = "摄像机采集部位类型")
	private String capturePositionType;

	@MessageElement("DeviceID")
	@Schema(description = "摄像机安装位置室外、室内属性。1-室外、2-室内。")
	private Integer roomType;

	// 2016
	@MessageElement("DeviceID")
	@Schema(description = "用途属性")
	private Integer useType;

	@MessageElement("DeviceID")
	@Schema(description = "摄像机补光属性。1-无补光;2-红外补光;3-白光补光;4-激光补光;9-其他")
	private Integer supplyLightType;

	@MessageElement("DeviceID")
	@Schema(description = "摄像机监视方位(光轴方向)属性。1-东(西向东)、2-西(东向西)、3-南(北向南)、4-北(南向北)、" +
			"5-东南(西北到东南)、6-东北(西南到东北)、7-西南(东北到西南)、8-西北(东南到西北)")
	private Integer directionType;

	@MessageElement("DeviceID")
	@Schema(description = "摄像机支持的分辨率,可多值")
	private String resolution;

	// 2022
	@MessageElement("DeviceID")
	@Schema(description = "摄像机支持的码流编号列表,用于实时点播时指定码流编号(可选)")
	private String streamNumberList;

	@MessageElement("DeviceID")
	@Schema(description = "下载倍速(可选),可多值")
	private String downloadSpeed;

	@MessageElement("DeviceID")
	@Schema(description = "空域编码能力,取值0-不支持;1-1级增强(1个增强层);2-2级增强(2个增强层);3-3级增强(3个增强层)")
	private Integer svcSpaceSupportMod;

	@MessageElement("DeviceID")
	@Schema(description = "时域编码能力,取值0-不支持;1-1级增强;2-2级增强;3-3级增强(可选)")
	private Integer svcTimeSupportMode;

	// 2022
	@MessageElement("DeviceID")
	@Schema(description = " SSVC增强层与基本层比例能力 ")
	private String ssvcRatioSupportList;

	// 2022
	@MessageElement("DeviceID")
	@Schema(description = "移动采集设备类型(仅移动采集设备适用,必选);1-移动机器人载摄像机;2-执法记录仪;3-移动单兵设备;" +
			"4-车载视频记录设备;5-无人机载摄像机;9-其他")
	private Integer mobileDeviceType;

	// 2022
	@MessageElement("DeviceID")
	@Schema(description = "摄像机水平视场角(可选),取值范围大于0度小于等于360度")
	private Double horizontalFieldAngle;

	// 2022
	@MessageElement("DeviceID")
	@Schema(description = "摄像机竖直视场角(可选),取值范围大于0度小于等于360度 ")
	private Double verticalFieldAngle;

	// 2022
	@MessageElement("DeviceID")
	@Schema(description = "摄像机可视距离(可选),单位:米")
	private Double maxViewDistance;

	// 2022
	@MessageElement("DeviceID")
	@Schema(description = "基层组织编码(必选,非基层建设时为“000000”)")
	private String grassrootsCode;

	// 2022
	@MessageElement("DeviceID")
	@Schema(description = "监控点位类型(当为摄像机时必选),1-一类视频监控点;2-二类视频监控点;3-三类视频监控点;9-其他点位。")
	private Integer poType;

	// 2022
	@MessageElement("DeviceID")
	@Schema(description = "点位俗称")
	private String poCommonName;

	// 2022
	@MessageElement("DeviceID")
	@Schema(description = "设备MAC地址(可选),用“XX-XX-XX-XX-XX-XX”格式表达")
	private String mac;

	// 2022
	@MessageElement("DeviceID")
	@Schema(description = "摄像机卡口功能类型,01-人脸卡口;02-人员卡口;03-机动车卡口;04-非机动车卡口;05-物品卡口;99-其他")
	private String functionType;

	// 2022
	@MessageElement("DeviceID")
	@Schema(description = "摄像机视频编码格式")
	private String encodeType;

	// 2022
	@MessageElement("DeviceID")
	@Schema(description = "摄像机安装使用时间")
	private String installTime;

	// 2022
	@MessageElement("DeviceID")
	@Schema(description = "摄像机所属管理单位名称")
	private String managementUnit;

	// 2022
	@MessageElement("DeviceID")
	@Schema(description = "摄像机所属管理单位联系人的联系方式(电话号码,可多值,用英文半角“/”分割)")
	private String contactInfo;

	// 2022
	@MessageElement("DeviceID")
	@Schema(description = "录像保存天数(可选)")
	private Integer recordSaveDays;

	// 2022
	@MessageElement("DeviceID")
	@Schema(description = "国民经济行业分类代码(可选)")
	private String industrialClassification;


	@Schema(description = "云台类型描述字符串")
	private String ptzTypeText;

	@Schema(description = "创建时间")
	private String createTime;

	@Schema(description = "更新时间")
	private String updateTime;

	@Schema(description = "子设备数")
	private int subCount;

	@Schema(description = "流唯一编号，存在表示正在直播")
	private String  streamId;

	@Schema(description = "是否含有音频")
	private Boolean hasAudio;

	@Schema(description = "GPS的更新时间")
	private String gpsTime;

	@Schema(description = "码流标识，优先级高于设备中码流标识，" +
			"用于选择码流时组成码流标识。默认为null，不设置。可选值: stream/streamnumber/streamprofile/streamMode")
	private String streamIdentification;

	public void setPtzType(int ptzType) {
		this.ptzType = ptzType;
		switch (ptzType) {
			case 0:
				this.ptzTypeText = "未知";
				break;
			case 1:
				this.ptzTypeText = "球机";
				break;
			case 2:
				this.ptzTypeText = "半球";
				break;
			case 3:
				this.ptzTypeText = "固定枪机";
				break;
			case 4:
				this.ptzTypeText = "遥控枪机";
				break;
			case 5:
				this.ptzTypeText = "遥控半球";
				break;
			case 6:
				this.ptzTypeText = "多目设备的全景/拼接通道";
				break;
			case 7:
				this.ptzTypeText = "多目设备的分割通道";
				break;
		}
	}
}
