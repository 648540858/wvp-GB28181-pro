package com.genersoft.iot.vmp.gb28181.bean;


import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

/**    
 * @description:设备录像信息bean 
 * @author: swwheihei
 * @date:   2020年5月8日 下午2:05:56     
 */
@Schema(description = "设备录像查询结果信息")
public class RecordInfo {

	@Schema(description = "设备编号")
	private String deviceId;

	@Schema(description = "通道编号")
	private String channelId;

	@Schema(description = "命令序列号")
	private String sn;

	@Schema(description = "设备名称")
	private String name;

	@Schema(description = "列表总数")
	private int sumNum;

	private int count;

	private Instant lastTime;

	@Schema(description = "")
	private List<RecordItem> recordList;

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

	public int getSumNum() {
		return sumNum;
	}

	public void setSumNum(int sumNum) {
		this.sumNum = sumNum;
	}

	public List<RecordItem> getRecordList() {
		return recordList;
	}

	public void setRecordList(List<RecordItem> recordList) {
		this.recordList = recordList;
	}

	public String getChannelId() {
		return channelId;
	}

	public void setChannelId(String channelId) {
		this.channelId = channelId;
	}

	public String getSn() {
		return sn;
	}

	public void setSn(String sn) {
		this.sn = sn;
	}

	public Instant getLastTime() {
		return lastTime;
	}

	public void setLastTime(Instant lastTime) {
		this.lastTime = lastTime;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}
}
