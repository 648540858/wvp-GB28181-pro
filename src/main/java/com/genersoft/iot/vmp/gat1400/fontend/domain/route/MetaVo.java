package com.genersoft.iot.vmp.gat1400.fontend.domain.route;

import java.io.Serializable;

import lombok.Data;

@Data
public class MetaVo implements Serializable {

    private static final long serialVersionUID=1L;

    /**
     * 路由标题
     */
    private String title;
    /**
     * 路由图标
     */
    private String icon;
}
