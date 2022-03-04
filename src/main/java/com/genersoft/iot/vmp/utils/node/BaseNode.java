package com.genersoft.iot.vmp.utils.node;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

/**
 * 节点基类
 *
 */
public class BaseNode<T> implements INode<T> {

	private static final long serialVersionUID = 1L;

	/**
	 * 主键ID
	 */
	protected int id;

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

	@Override
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	@Override
	public String getParentId() {
		return parentId;
	}

	public void setParentId(String parentId) {
		this.parentId = parentId;
	}

	@Override
	public List<T> getChildren() {
		return children;
	}

	public void setChildren(List<T> children) {
		this.children = children;
	}

	public void setHasChildren(Boolean hasChildren) {
		this.hasChildren = hasChildren;
	}
}
