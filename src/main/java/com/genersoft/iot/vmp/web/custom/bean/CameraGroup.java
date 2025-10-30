package com.genersoft.iot.vmp.web.custom.bean;

import com.genersoft.iot.vmp.gb28181.bean.Group;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

public class CameraGroup extends Group {

    @Getter
    private CameraGroup parent;

    @Getter
    private final List<CameraGroup> child = new ArrayList<>();

    public void setParent(CameraGroup parent) {
        if (parent == null) {
            return;
        }
        this.parent = parent;
        parent.addChild(this);
    }

    public void addChild(CameraGroup child) {
        if (child == null) {
            return;
        }
        this.child.add(child);
        if (this.parent != null) {
            this.parent.addChild(child);
        }
    }
}
