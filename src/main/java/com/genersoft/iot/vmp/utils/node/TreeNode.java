package com.genersoft.iot.vmp.utils.node;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 树型节点类
 *
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class TreeNode extends BaseNode<TreeNode> {

	private static final long serialVersionUID = 1L;

	private String title;

	private String key;

	private String value;
}
