package com.genersoft.iot.vmp.gb28181.bean;

/**    
 * @Description:设备录像bean 
 * @author: songww
 * @date:   2020年5月8日 下午2:06:54     
 */
public class RecordItem {

	private String deviceId;
	
	private String name;
	
	private String filePath;
	
	private String address;
	
	private String startTime;
	
	private String endTime;
	
	private int secrecy;
	
	private String type;
	
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

	public void setRecordId(String recorderId) {
		this.recorderId = recorderId;
	}

	public String getEndTime() {
		return endTime;
	}

	public void setEndTime(String endTime) {
		this.endTime = endTime;
	}
}
