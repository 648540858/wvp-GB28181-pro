package com.genersoft.iot.vmp.utils.node;

import java.io.Serializable;
import java.util.List;

/**
 *
 * 节点
 */
public interface INode<T> extends Serializable {

	/**
	 * 主键
	 *
	 * @return String
	 */
	String getId();

	/**
	 * 父主键
	 *
	 * @return String
	 */
	String getParentId();

	/**
	 * 子孙节点
	 *
	 * @return List<T>
	 */
	List<T> getChildren();

	/**
	 * 是否有子孙节点
	 *
	 * @return Boolean
	 */
	default Boolean getHasChildren() {
		return false;
	}

}
