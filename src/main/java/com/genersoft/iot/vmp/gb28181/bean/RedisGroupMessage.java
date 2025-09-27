package com.genersoft.iot.vmp.gb28181.bean;

import lombok.Data;

@Data
public class RedisGroupMessage {

    /**
     * 分组国标ID
     */
    private String groupGbId;

    /**
     * 分组别名
     */
    private String groupAlias;

    /**
     * 分组名称
     */
    private String groupName;

    /**
     * 分组所属的行政区划
     */
    private String groupCivilCode;

    /**
     * 分组所属父分组国标ID
     */
    private String parentGroupGbId;

    /**
     * 分组所属父分组别名
     */
    private String parentGAlias;

    /**
     * 分组所属业务分组国标ID
     */
    private String topGroupGbId;

    /**
     * 分组所属业务分组别名
     */
    private String topGroupGAlias;

    /**
     * 分组变化消息中的消息类型，取值为 add update delete
     */
    private String messageType;


    @Override
    public String toString() {
        return "RedisGroupMessage{" +
                "groupGbId='" + groupGbId + '\'' +
                ", groupAlias='" + groupAlias + '\'' +
                ", groupName='" + groupName + '\'' +
                ", groupCivilCode='" + groupCivilCode + '\'' +
                ", parentGroupGbId='" + parentGroupGbId + '\'' +
                ", parentGAlias='" + parentGAlias + '\'' +
                ", topGroupGbId='" + topGroupGbId + '\'' +
                ", topGroupGAlias='" + topGroupGAlias + '\'' +
                '}';
    }
}
