package com.genersoft.iot.vmp.gb28181.event.subscribe.catalog;

import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

import java.util.List;

public class CatalogEvent  extends ApplicationEvent {

    public CatalogEvent(Object source) {
        super(source);
    }

    /**
     * 上线
     */
    public static final String ON = "ON";

    /**
     * 离线
     */
    public static final String OFF = "OFF";

    /**
     * 视频丢失
     */
    public static final String VLOST = "VLOST";

    /**
     * 故障
     */
    public static final String DEFECT = "DEFECT";

    /**
     * 增加
     */
    public static final String ADD = "ADD";

    /**
     * 删除
     */
    public static final String DEL = "DEL";

    /**
     * 更新
     */
    public static final String UPDATE = "UPDATE";

    @Setter
    @Getter
    private List<CommonGBChannel> channels;

    @Setter
    @Getter
    private String type;

    @Setter
    @Getter
    private Integer platformId;

}
