package com.genersoft.iot.vmp.gb28181.bean;

import com.alibaba.fastjson2.JSON;
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


    public static void main(String[] args) {
        RedisGroupMessage redisGroupMessage = new RedisGroupMessage();
        redisGroupMessage.setGroupAlias("100000001");
        redisGroupMessage.setGroupName("消防大队");
        System.out.println(JSON.toJSONString(redisGroupMessage));
    }


}
