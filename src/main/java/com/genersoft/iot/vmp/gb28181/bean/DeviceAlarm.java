package com.genersoft.iot.vmp.gb28181.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

/**
 * @author lin
 */
@Schema(description = "报警信息")
@Data
public class DeviceAlarm {

    /**
     * 数据库id
     */
    @Schema(description = "数据库id")
    private String id;

    /**
     * 设备Id
     */
    @Schema(description = "设备的国标编号")
    private String deviceId;

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

    public String getAlarmPriorityDescription() {
        switch (alarmPriority) {
            case "1":
                return "一级警情";
            case "2":
                return "二级警情";
            case "3":
                return "三级警情";
            case "4":
                return "四级警情";
            default:
                return alarmPriority;
        }
    }

    /**
     * 报警方式 , 1为电话报警, 2为设备报警, 3为短信报警, 4为 GPS报警, 5为视频报警, 6为设备故障报警,
     * 7其他报警;可以为直接组合如12为电话报警或 设备报警-
     */
    @Schema(description = "报警方式 , 1为电话报警, 2为设备报警, 3为短信报警, 4为 GPS报警, 5为视频报警, 6为设备故障报警,\n" +
            "\t * 7其他报警;可以为直接组合如12为电话报警或设备报警")
    private String alarmMethod;


    private String alarmMethodDescription;

    public String getAlarmMethodDescription() {
        StringBuilder stringBuilder = new StringBuilder();
        char[] charArray = alarmMethod.toCharArray();
        for (char c : charArray) {
            switch (c) {
                case '1':
                    stringBuilder.append("-电话报警");
                    break;
                case '2':
                    stringBuilder.append("-设备报警");
                    break;
                case '3':
                    stringBuilder.append("-短信报警");
                    break;
                case '4':
                    stringBuilder.append("-GPS报警");
                    break;
                case '5':
                    stringBuilder.append("-视频报警");
                    break;
                case '6':
                    stringBuilder.append("-设备故障报警");
                    break;
                case '7':
                    stringBuilder.append("-其他报警");
                    break;
            }
        }
		stringBuilder.delete(0, 1);
        return stringBuilder.toString();
    }

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
    private String alarmType;

	public String getAlarmTypeDescription() {
		if (alarmType == null) {
			return "";
		}
		char[] charArray = alarmMethod.toCharArray();
		Set<String> alarmMethodSet = new HashSet<>();
		for (char c : charArray) {
			alarmMethodSet.add(Character.toString(c));
		}
		String result = alarmType;
		if (alarmMethodSet.contains("2")) {
			switch (alarmType) {
				case "1":
					result = "视频丢失报警";
					break;
				case "2":
					result = "设备防拆报警";
					break;
				case "3":
					result = "存储设备磁盘满报警";
					break;
				case "4":
					result = "设备高温报警";
					break;
				case "5":
					result = "设备低温报警";
					break;
			}
		}
		if (alarmMethodSet.contains("5")) {
			switch (alarmType) {
				case "1":
					result = "人工视频报警";
					break;
				case "2":
					result = "运动目标检测报警";
					break;
				case "3":
					result = "遗留物检测报警";
					break;
				case "4":
					result = "物体移除检测报警";
					break;
				case "5":
					result = "绊线检测报警";
					break;
				case "6":
					result = "入侵检测报警";
					break;
				case "7":
					result = "逆行检测报警";
					break;
				case "8":
					result = "徘徊检测报警";
					break;
				case "9":
					result = "流量统计报警";
					break;
				case "10":
					result = "密度检测报警";
					break;
				case "11":
					result = "视频异常检测报警";
					break;
				case "12":
					result = "快速移动报警";
					break;
			}
		}
		if (alarmMethodSet.contains("6")) {
			switch (alarmType) {
				case "1":
					result = "人工视频报警";
					break;
				case "2":
					result = "运动目标检测报警";
					break;
				case "3":
					result = "遗留物检测报警";
					break;
				case "4":
					result = "物体移除检测报警";
					break;
				case "5":
					result = "绊线检测报警";
					break;
				case "6":
					result = "入侵检测报警";
					break;
				case "7":
					result = "逆行检测报警";
					break;
				case "8":
					result = "徘徊检测报警";
					break;
				case "9":
					result = "流量统计报警";
					break;
				case "10":
					result = "密度检测报警";
					break;
				case "11":
					result = "视频异常检测报警";
					break;
				case "12":
					result = "快速移动报警";
					break;
			}
		}
		return result;
	}

	@Schema(description = "报警类型描述")
    private String alarmTypeDescription;

    @Schema(description = "创建时间")
    private String createTime;

}
