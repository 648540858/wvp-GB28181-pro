package com.genersoft.iot.vmp.vmanager.bean;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.utils.node.INode;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel(value = "DeviceChannelTree对象", description = "DeviceChannelTree对象")
public class DeviceChannelTree extends DeviceChannel implements INode<DeviceChannelTree> {
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private String id;

    /**
     * 父节点ID
     */
    private String parentId;

    private String parentName;

    private String title;

    private String key;

    private String value;

    /**
     * 子孙节点
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<DeviceChannelTree> children;

    /**
     * 是否有子孙节点
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Boolean hasChildren;

    @Override
    public List<DeviceChannelTree> getChildren() {
        if (this.children == null) {
            this.children = new ArrayList<>();
        }
        return this.children;
    }

    @Override
    public Boolean getHasChildren() {
        if (children.size() > 0) {
            return true;
        } else {
            return this.hasChildren;
        }
    }
}
