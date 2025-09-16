package com.genersoft.iot.vmp.media.abl.bean;

import lombok.Data;

@Data
public class ABLRecordFile {
    private String file;
    private Long duration;
    private ABLUrls url;
}
