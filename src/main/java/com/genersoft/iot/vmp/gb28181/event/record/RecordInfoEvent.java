package com.genersoft.iot.vmp.gb28181.event.record;

import com.genersoft.iot.vmp.gb28181.bean.RecordInfo;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

/**
 * @description: 录像查询结束时间
 * @author: pan
 * @data: 2022-02-23
 */

@Setter
@Getter
public class RecordInfoEvent extends ApplicationEvent {
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public RecordInfoEvent(Object source) {
        super(source);
    }

    private RecordInfo recordInfo;
}
