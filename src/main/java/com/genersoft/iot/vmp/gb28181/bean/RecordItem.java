package com.genersoft.iot.vmp.gb28181.bean;


import com.genersoft.iot.vmp.utils.DateUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.temporal.TemporalAccessor;

/**
 * @description:设备录像bean 
 * @author: swwheihei
 * @date:   2020年5月8日 下午2:06:54     
 */
@Schema(description = "设备录像详情")
public class RecordItem  implements Comparable<RecordItem>{

	@Schema(description = "设备编号")
	private String deviceId;

	@Schema(description = "名称")
	private String name;

	@Schema(description = "文件路径名 (可选)")
	private String filePath;

	@Schema(description = "录像文件大小,单位:Byte(可选)")
	private String fileSize;

	@Schema(description = "录像地址(可选)")
	private String address;

	@Schema(description = "录像开始时间(可选)")
	private String startTime;

	@Schema(description = "录像结束时间(可选)")
	private String endTime;

	@Schema(description = "保密属性(必选)缺省为0;0:不涉密,1:涉密")
	private int secrecy;

	@Schema(description = "录像产生类型(可选)time或alarm 或 manua")
	private String type;

	@Schema(description = "录像触发者ID(可选)")
	private String recorderId;

	public String getDeviceId() {
		return deviceId;
	}

	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getStartTime() {
		return startTime;
	}

	public void setStartTime(String startTime) {
		this.startTime = startTime;
	}

	public String getEndTime() {
		return endTime;
	}

	public void setEndTime(String endTime) {
		this.endTime = endTime;
	}

	public int getSecrecy() {
		return secrecy;
	}

	public void setSecrecy(int secrecy) {
		this.secrecy = secrecy;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getRecorderId() {
		return recorderId;
	}

	public void setRecorderId(String recorderId) {
		this.recorderId = recorderId;
	}

	public String getFileSize() {
		return fileSize;
	}

	public void setFileSize(String fileSize) {
		this.fileSize = fileSize;
	}

	@Override
	public int compareTo(@NotNull RecordItem recordItem) {
		TemporalAccessor startTimeNow = DateUtil.formatter.parse(startTime);
		TemporalAccessor startTimeParam = DateUtil.formatter.parse(recordItem.getStartTime());
		Instant startTimeParamInstant = Instant.from(startTimeParam);
		Instant startTimeNowInstant = Instant.from(startTimeNow);
		if (startTimeNowInstant.equals(startTimeParamInstant)) {
			return 0;
		}else if (Instant.from(startTimeParam).isAfter(Instant.from(startTimeNow)) ) {
			return -1;
		}else {
			return 1;
		}

	}
}
