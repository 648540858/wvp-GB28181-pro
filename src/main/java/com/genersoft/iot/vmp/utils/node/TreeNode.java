package com.genersoft.iot.vmp.utils.node;



/**
 * 树型节点类
 *
 */
public class TreeNode extends BaseNode<TreeNode> {

	private static final long serialVersionUID = 1L;

	private String title;

	private String key;

	private String value;

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
}
