package com.genersoft.iot.vmp.gat1400.framework.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.genersoft.iot.vmp.gat1400.framework.StatusCodeDeSerializer;
import com.genersoft.iot.vmp.gat1400.framework.StatusCodeSerializer;

import org.apache.commons.lang3.time.DateFormatUtils;

import java.util.Date;

import lombok.Data;

@Data
public class ResponseStatusObject {

    @JsonProperty("Id")
    private String id;
    @JsonProperty("RequestURL")
    private String requestUrl;
    @JsonSerialize(using = StatusCodeSerializer.class)
    @JsonDeserialize(using = StatusCodeDeSerializer.class)
    @JsonProperty("StatusCode")
    private String statusCode;
    @JsonProperty("StatusString")
    private String statusString;
    @JsonProperty("LocalTime")
    private String localTime;

    public ResponseStatusObject() {
    }

    public ResponseStatusObject(String requestUrl, String statusCode, String statusString) {
        this(null, requestUrl, statusCode, statusString);
    }

    public ResponseStatusObject(String id, String requestUrl, String statusCode, String statusString) {
        this.id = id;
        this.requestUrl = requestUrl;
        this.statusCode = statusCode;
        this.statusString = statusString;
        this.localTime = DateFormatUtils.format(new Date(), "yyyyMMddHHmmss");
    }

}
