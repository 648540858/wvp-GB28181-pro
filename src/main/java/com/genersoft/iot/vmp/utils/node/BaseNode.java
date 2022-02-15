package com.genersoft.iot.vmp.utils.node;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 节点基类
 *
 */
@Data
public class BaseNode<T> implements INode<T> {

	private static final long serialVersionUID = 1L;

	/**
	 * 主键ID
	 */
	protected String id;

	/**
	 * 父节点ID
	 */
	protected String parentId;

	/**
	 * 子孙节点
	 */
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	protected List<T> children = new ArrayList<T>();

	/**
	 * 是否有子孙节点
	 */
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private Boolean hasChildren;

	/**
	 * 是否有子孙节点
	 *
	 * @return Boolean
	 */
	@Override
	public Boolean getHasChildren() {
		if (children.size() > 0) {
			return true;
		} else {
			return this.hasChildren;
		}
	}

}
