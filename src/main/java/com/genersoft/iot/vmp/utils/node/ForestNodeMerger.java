package com.genersoft.iot.vmp.utils.node;

import com.genersoft.iot.vmp.utils.CollectionUtil;

import java.util.List;

/**
 * 森林节点归并类
 *
 */
public class ForestNodeMerger {

	/**
	 * 将节点数组归并为一个森林（多棵树）（填充节点的children域）
	 * 时间复杂度为O(n^2)
	 *
	 * @param items 节点域
	 * @return 多棵树的根节点集合
	 */
	public static <T extends INode<T>> List<T> merge(List<T> items) {
		ForestNodeManager<T> forestNodeManager = new ForestNodeManager<>(items);
		items.forEach(forestNode -> {
			if (forestNode.getParentId() != null) {
				INode<T> node = forestNodeManager.getTreeNodeAt(forestNode.getParentId());
				if (node != null) {
					node.getChildren().add(forestNode);
				} else {
					forestNodeManager.addParentId(forestNode.getId());
				}
			}
		});
		return forestNodeManager.getRoot();
	}

	public static <T extends INode<T>> List<T> merge(List<T> items, String[] parentIds) {
		ForestNodeManager<T> forestNodeManager = new ForestNodeManager<>(items);
		items.forEach(forestNode -> {
			if (forestNode.getParentId() != null) {
				INode<T> node = forestNodeManager.getTreeNodeAt(forestNode.getParentId());
				if (CollectionUtil.contains(parentIds, forestNode.getId())){
					forestNodeManager.addParentId(forestNode.getId());
				} else {
					if (node != null){
						node.getChildren().add(forestNode);
					}
				}
			}
		});
		return forestNodeManager.getRoot();
	}
}
