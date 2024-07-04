package com.genersoft.iot.vmp.gb28181.bean;

import com.genersoft.iot.vmp.gb28181.utils.MessageElement;
import com.genersoft.iot.vmp.gb28181.utils.XmlUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.Element;

import java.lang.reflect.InvocationTargetException;

@Data
@Slf4j
@Schema(description = "通道信息")
@EqualsAndHashCode(callSuper = true)
public class DeviceChannel extends CommonGBChannel {

	@Schema(description = "数据库自增ID")
	private int id;

	@Schema(description = "设备的数据库自增ID")
	private Integer deviceDbId;

	@MessageElement("DeviceID")
	@Schema(description = "编码")
	private String deviceId;

	@MessageElement("Name")
	@Schema(description = "名称")
	private String name;

	@MessageElement("Manufacturer")
	@Schema(description = "设备厂商")
	private String manufacturer;

	@MessageElement("Model")
	@Schema(description = "设备型号")
	private String model;

	// 2016
	@MessageElement("Owner")
	@Schema(description = "设备归属")
	private String owner;

	@MessageElement("CivilCode")
	@Schema(description = "行政区域")
	private String civilCode;

	@MessageElement("Block")
	@Schema(description = "警区")
	private String block;

	@MessageElement("Address")
	@Schema(description = "安装地址")
	private String address;

	@MessageElement("Parental")
	@Schema(description = "是否有子设备(必选)1有,0没有")
	private Integer parental;


	@MessageElement("ParentID")
	@Schema(description = "父节点ID")
	private String parentId;

	// 2016
	@MessageElement("SafetyWay")
	@Schema(description = "信令安全模式")
	private Integer safetyWay;

	@MessageElement("RegisterWay")
	@Schema(description = "注册方式")
	private Integer registerWay;

	// 2016
	@MessageElement("CertNum")
	@Schema(description = "证书序列号")
	private String certNum;

	// 2016
	@MessageElement("Certifiable")
	@Schema(description = "证书有效标识, 缺省为0;证书有效标识:0:无效 1:有效")
	private Integer certifiable;

	// 2016
	@MessageElement("ErrCode")
	@Schema(description = "无效原因码(有证书且证书无效的设备必选)")
	private Integer errCode;

	// 2016
	@MessageElement("EndTime")
	@Schema(description = "证书终止有效期(有证书且证书无效的设备必选)")
	private String endTime;

	@MessageElement("Secrecy")
	@Schema(description = "保密属性(必选)缺省为0;0-不涉密,1-涉密")
	private Integer secrecy;

	@MessageElement("IPAddress")
	@Schema(description = "设备/系统IPv4/IPv6地址")
	private String ipAddress;

	@MessageElement("Port")
	@Schema(description = "设备/系统端口")
	private Integer port;

	@MessageElement("Password")
	@Schema(description = "设备口令")
	private String password;

	@MessageElement("Status")
	@Schema(description = "设备状态")
	private String status;

	@MessageElement("Longitude")
	@Schema(description = "经度 WGS-84坐标系")
	private Double longitude;


	@MessageElement("Latitude")
	@Schema(description = ",纬度 WGS-84坐标系")
	private Double latitude;

	@MessageElement("Info.PTZType")
	@Schema(description = "摄像机结构类型,标识摄像机类型: 1-球机; 2-半球; 3-固定枪机; 4-遥控枪机;5-遥控半球;6-多目设备的全景/拼接通道;7-多目设备的分割通道")
	private Integer ptzType;

	@MessageElement("Info.PositionType")
	@Schema(description = "摄像机位置类型扩展。1-省际检查站、2-党政机关、3-车站码头、4-中心广场、5-体育场馆、" +
			"6-商业中心、7-宗教场所、8-校园周边、9-治安复杂区域、10-交通干线")
	private Integer positionType;

	@MessageElement("Info.RoomType")
	@Schema(description = "摄像机安装位置室外、室内属性。1-室外、2-室内。")
	private Integer roomType;

	@MessageElement("Info.UseType")
	@Schema(description = "用途属性， 1-治安、2-交通、3-重点。")
	private Integer useType;

	@MessageElement("Info.SupplyLightType")
	@Schema(description = "摄像机补光属性。1-无补光;2-红外补光;3-白光补光;4-激光补光;9-其他")
	private Integer supplyLightType;

	@MessageElement("Info.DirectionType")
	@Schema(description = "摄像机监视方位(光轴方向)属性。1-东(西向东)、2-西(东向西)、3-南(北向南)、4-北(南向北)、" +
			"5-东南(西北到东南)、6-东北(西南到东北)、7-西南(东北到西南)、8-西北(东南到西北)")
	private Integer directionType;

	@MessageElement("Info.Resolution")
	@Schema(description = "摄像机支持的分辨率,可多值")
	private String resolution;

	@MessageElement("Info.BusinessGroupID")
	@Schema(description = "虚拟组织所属的业务分组ID")
	private String businessGroupId;

	@MessageElement("Info.DownloadSpeed")
	@Schema(description = "下载倍速(可选),可多值")
	private String downloadSpeed;

	@MessageElement("Info.SVCSpaceSupportMode")
	@Schema(description = "空域编码能力,取值0-不支持;1-1级增强(1个增强层);2-2级增强(2个增强层);3-3级增强(3个增强层)")
	private Integer svcSpaceSupportMod;

	@MessageElement("Info.SVCTimeSupportMode")
	@Schema(description = "时域编码能力,取值0-不支持;1-1级增强;2-2级增强;3-3级增强(可选)")
	private Integer svcTimeSupportMode;

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

	public static DeviceChannel decode(Element element) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        return XmlUtil.elementDecode(element, DeviceChannel.class);
	}

	public static DeviceChannel decodeWithOnlyDeviceId(Element element) {
		Element deviceElement = element.element("DeviceID");
		DeviceChannel deviceChannel = new DeviceChannel();
		deviceChannel.setDeviceId(deviceElement.getText());
		return deviceChannel;
	}


}
