package com.genersoft.iot.vmp.gat1400.fontend.domain;

import com.genersoft.iot.vmp.gat1400.framework.domain.core.PaginationRequest;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PublishQuery extends PaginationRequest {

    private String title;
    private String subscribeDetail;
    private String serverId;
}
