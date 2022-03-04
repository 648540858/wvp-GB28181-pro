package com.genersoft.iot.vmp.vmanager.bean;

import com.genersoft.iot.vmp.utils.node.TreeNode;

public class DeviceChannelTreeNode extends TreeNode {

	private Integer status;

	private String deviceId;

	private String channelId;

	private Double lng;

	private Double lat;

	public Integer getStatus() {
		return status;
	}

	public void setStatus(Integer status) {
		this.status = status;
	}

	public String getDeviceId() {
		return deviceId;
	}

	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}

	public String getChannelId() {
		return channelId;
	}

	public void setChannelId(String channelId) {
		this.channelId = channelId;
	}

	public Double getLng() {
		return lng;
	}

	public void setLng(Double lng) {
		this.lng = lng;
	}

	public Double getLat() {
		return lat;
	}

	public void setLat(Double lat) {
		this.lat = lat;
	}
}
