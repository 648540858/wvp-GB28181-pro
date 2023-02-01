package com.genersoft.iot.vmp.gb28181.bean;

public class DeviceChannelInPlatform extends DeviceChannel{

	private String platFormId;
	private String catalogId;

	public String getPlatFormId() {
		return platFormId;
	}

	public void setPlatFormId(String platFormId) {
		this.platFormId = platFormId;
	}

	public String getCatalogId() {
		return catalogId;
	}

	public void setCatalogId(String catalogId) {
		this.catalogId = catalogId;
	}
}
