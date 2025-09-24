package com.genersoft.iot.vmp.media.zlm.dto;

import lombok.Data;

@Data
public class SessionData {
    private String id;
    private String local_ip;
    private Integer local_port;
    private String peer_ip;
    private Integer peer_port;
    private String typeid;

}
