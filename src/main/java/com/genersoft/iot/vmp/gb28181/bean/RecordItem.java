package com.genersoft.iot.vmp.gb28181.bean;

import lombok.Data;

/**
 * @Description:设备录像bean 
 * @author: swwheihei
 * @date:   2020年5月8日 下午2:06:54     
 */

@Data
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

}
