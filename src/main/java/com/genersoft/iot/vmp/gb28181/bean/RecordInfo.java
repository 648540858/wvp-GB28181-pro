package com.genersoft.iot.vmp.gb28181.bean;

import java.time.Instant;
import java.util.List;

/**    
 * @description:设备录像信息bean 
 * @author: swwheihei
 * @date:   2020年5月8日 下午2:05:56     
 */
public class RecordInfo {

	private String deviceId;

	private String channelId;

	private String sn;

	private String name;
	
	private int sumNum;

	private Instant lastTime;
	
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
}
