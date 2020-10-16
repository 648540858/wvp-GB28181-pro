package com.genersoft.iot.vmp.gb28181.bean;

import lombok.Data;

import java.util.List;

/**    
 * @Description:设备录像信息bean 
 * @author: swwheihei
 * @date:   2020年5月8日 下午2:05:56     
 */
@Data
public class RecordInfo {

	private String deviceId;
	
	private String name;
	
	private int sumNum;
	
	private List<RecordItem> recordList;

}
