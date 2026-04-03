package com.genersoft.iot.vmp.gb28181.bean;

import com.genersoft.iot.vmp.gb28181.utils.XmlUtil;
import com.genersoft.iot.vmp.service.bean.AlarmType;
import com.genersoft.iot.vmp.utils.DateUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.dom4j.Element;

import static com.genersoft.iot.vmp.gb28181.utils.XmlUtil.*;

/**
 * @author lin
 */
@Schema(description = "报警通知")
@Data
public class DeviceAlarmNotify {

    @Schema(description = "设备的国标编号")
    private String deviceId;

	@Schema(description = "设备名称")
	private String deviceName;

    /**
     * 通道Id
     */
    @Schema(description = "通道的国标编号")
    private String channelId;

    /**
     * 报警级别, 1为一级警情, 2为二级警情, 3为三级警情, 4为四级警情
     */
    @Schema(description = "报警级别, 1为一级警情, 2为二级警情, 3为三级警情, 4为四级警情")
    private String alarmPriority;

    @Schema(description = "报警级别, 1为一级警情, 2为二级警情, 3为三级警情, 4为四级警情")
    private String alarmPriorityDescription;

    /**
     * 报警方式 , 1为电话报警, 2为设备报警, 3为短信报警, 4为 GPS报警, 5为视频报警, 6为设备故障报警,
     * 7其他报警;可以为直接组合如12为电话报警或 设备报警-
     */
    @Schema(description = "报警方式 , 1为电话报警, 2为设备报警, 3为短信报警, 4为 GPS报警, 5为视频报警, 6为设备故障报警,\n" +
            "\t * 7其他报警;可以为直接组合如12为电话报警或设备报警")
    private Integer alarmMethod;


    private String alarmMethodDescription;


    /**
     * 报警时间
     */
    @Schema(description = "报警时间")
    private String alarmTime;

    /**
     * 报警内容描述
     */
    @Schema(description = "报警内容描述")
    private String alarmDescription;

    /**
     * 经度
     */
    @Schema(description = "经度")
    private double longitude;

    /**
     * 纬度
     */
    @Schema(description = "纬度")
    private double latitude;

    /**
     * 报警类型,
     * 报警方式为2时,不携带 AlarmType为默认的报警设备报警,
     * 携带 AlarmType取值及对应报警类型如下:
     * 1-视频丢失报警;
     * 2-设备防拆报警;
     * 3-存储设备磁盘满报警;
     * 4-设备高温报警;
     * 5-设备低温报警。
     * 报警方式为5时,取值如下:
     * 1-人工视频报警;
     * 2-运动目标检测报警;
     * 3-遗留物检测报警;
     * 4-物体移除检测报警;
     * 5-绊线检测报警;
     * 6-入侵检测报警;
     * 7-逆行检测报警;
     * 8-徘徊检测报警;
     * 9-流量统计报警;
     * 10-密度检测报警;
     * 11-视频异常检测报警;
     * 12-快速移动报警。
     * 报警方式为6时,取值下:
     * 1-存储设备磁盘故障报警;
     * 2-存储设备风扇故障报警。
     */
    @Schema(description = "报警类型")
    private Integer alarmType;

    @Schema(description = "事件类型, 在入侵检测报警时可携带")
    private Integer eventType;

	public AlarmType getAlarmTypeEnum() {
		if (alarmType == null) {
			return null;
		}

		if (alarmMethod == DeviceAlarmMethod.Device.getVal()) {
			// 2为设备报警,
			// 报警方式为2时,
			// 不携带 AlarmType为默认的报警设备报警,
			// 携带 AlarmType取值及对应报警类型如下:
			// 1-视频丢失报警;2-设备防拆报警;3-存储设备磁盘满报警;4-设备高温报警;5-设备低温报警
			switch (alarmType) {
				case 1:
					return AlarmType.VideoLoss;
				case 2:
					return AlarmType.DeviceTamper;
				case 3:
					return AlarmType.StorageFull;
				case 4:
					return AlarmType.DeviceHighTemperature;
				case 5:
					return AlarmType.DeviceLowTemperature;
			}
		}
		if (alarmMethod == DeviceAlarmMethod.GPS.getVal()) {
			// 5为视频报警
			// 报警方式为5时,
			// 取值如下:
			// 1-人工视频报警;2-运动目标检测报警;3-遗留物检测报警;4-物体移除检测报警;5-绊线检测报警;
			// 6-入侵检测报警;7-逆行检测报警;8-徘徊检测报警;9-流量统计报警;
			// 10-密度检测报警;11-视频异常检测报警;12-快速移动报警。
			switch (alarmType) {
				case 1:
					return AlarmType.ManualVideo;
				case 2:
					return AlarmType.MotionDetection;
				case 3:
					return AlarmType.LeftObjectDetection;
				case 4:
					return AlarmType.ObjectRemovalDetection;
				case 5:
					return AlarmType.TripwireDetection;
				case 6:
					return AlarmType.IntrusionDetection;
				case 7:
					return AlarmType.ReverseDetection;
				case 8:
					return AlarmType.LoiteringDetection;
				case 9:
					return AlarmType.FlowStatistics;
				case 10:
					return AlarmType.DensityDetection;
				case 11:
					return AlarmType.VideoAbnormal;
				case 12:
					return AlarmType.RapidMovement;
			}
		}
		if (alarmMethod == DeviceAlarmMethod.DeviceFailure.getVal()) {
			switch (alarmType) {
				case 1:
					return AlarmType.StorageFault;
				case 2:
					return AlarmType.StorageFanFault;
			}
		}
		return null;
	}

	@Schema(description = "报警类型描述")
    private String alarmTypeDescription;

    @Schema(description = "创建时间")
    private String createTime;


	public static DeviceAlarmNotify fromXml(Element rootElement) {
		Element deviceIdElement = rootElement.element("DeviceID");
		String channelId = deviceIdElement.getText();

		DeviceAlarmNotify deviceAlarm = new DeviceAlarmNotify();
		deviceAlarm.setCreateTime(DateUtil.getNow());
		deviceAlarm.setChannelId(channelId);
		deviceAlarm.setAlarmPriority(getText(rootElement, "AlarmPriority"));
		deviceAlarm.setAlarmMethod(getInteger(rootElement, "AlarmMethod"));
		String alarmTime = XmlUtil.getText(rootElement, "AlarmTime");
		deviceAlarm.setAlarmTime(DateUtil.ISO8601Toyyyy_MM_dd_HH_mm_ss(alarmTime));
		deviceAlarm.setAlarmDescription(getText(rootElement, "AlarmDescription"));

		Double longitude = getDouble(rootElement, "Longitude");
		deviceAlarm.setLongitude(longitude != null ? longitude: 0.00D);
		Double latitude = getDouble(rootElement, "Latitude");
		deviceAlarm.setLatitude(latitude != null ? latitude: 0.00D);
		deviceAlarm.setAlarmType(getInteger(rootElement, "AlarmType"));
		Element info = rootElement.element("Info");
		if (info != null) {
			deviceAlarm.setAlarmType(getInteger(info, "AlarmType"));
			Element alarmTypeParam = info.element("AlarmTypeParam");
			if (alarmTypeParam != null) {
				deviceAlarm.setAlarmDescription(alarmTypeParam.elementText("AlarmDescription"));
			}
		}
		deviceAlarm.setCreateTime(DateUtil.getNow());
		return deviceAlarm;
	}

}
