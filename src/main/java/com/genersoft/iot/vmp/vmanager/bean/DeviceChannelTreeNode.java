package com.genersoft.iot.vmp.vmanager.bean;

import com.genersoft.iot.vmp.utils.node.TreeNode;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class DeviceChannelTreeNode extends TreeNode {

	private Integer status;

	private String deviceId;

	private String channelId;

	private Double lng;

	private Double lat;
}
