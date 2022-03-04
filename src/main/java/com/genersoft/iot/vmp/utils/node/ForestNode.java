package com.genersoft.iot.vmp.utils.node;



/**
 * 森林节点类
 *
 */
public class ForestNode extends BaseNode<ForestNode> {

	private static final long serialVersionUID = 1L;

	/**
	 * 节点内容
	 */
	private Object content;

	public ForestNode(int id, String parentId, Object content) {
		this.id = id;
		this.parentId = parentId;
		this.content = content;
	}

	public Object getContent() {
		return content;
	}

	public void setContent(Object content) {
		this.content = content;
	}
}
