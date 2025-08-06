package com.genersoft.iot.vmp.gb28181.bean;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

/**
 * @description:设备录像信息bean
 * @author: swwheihei
 * @date:   2020年5月8日 下午2:05:56
 */
@Setter
@Getter
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

}
