package com.genersoft.iot.vmp.vmanager.bean;

import org.jetbrains.annotations.NotNull;

import java.text.Collator;
import java.util.Comparator;

/**
 * @author lin
 */
public class BaseTree<T> implements Comparable<BaseTree>{
    private String id;

    private String deviceId;
    private String pid;
    private String name;
    private boolean parent;

    private T basicData;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public T getBasicData() {
        return basicData;
    }

    public void setBasicData(T basicData) {
        this.basicData = basicData;
    }

    public boolean isParent() {
        return parent;
    }

    public void setParent(boolean parent) {
        this.parent = parent;
    }

    @Override
    public int compareTo(@NotNull BaseTree treeNode) {
        if (this.parent || treeNode.isParent()) {
            if (!this.parent && !treeNode.isParent()) {
                Comparator cmp = Collator.getInstance(java.util.Locale.CHINA);
                return cmp.compare(treeNode.getName(), this.getName());
            }else {
                if (this.isParent()) {
                    return 1;
                }else {
                    return -1;
                }
            }
        }else{
            Comparator cmp = Collator.getInstance(java.util.Locale.CHINA);
            return cmp.compare(treeNode.getName(), this.getName());
        }
    }
}
