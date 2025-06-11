package com.genersoft.iot.vmp.gat1400.framework.domain.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.genersoft.iot.vmp.gat1400.framework.domain.dto.ResponseStatusListObject;
import com.genersoft.iot.vmp.gat1400.framework.domain.dto.ResponseStatusObject;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VIIDResponseStatusObject {

    @JsonProperty("ResponseStatusListObject")
    private ResponseStatusListObject responseStatusListObject;

    public static VIIDResponseStatusObject from(ResponseStatusObject... statusObjects) {
        VIIDResponseStatusObject object = new VIIDResponseStatusObject();
        ResponseStatusListObject response = new ResponseStatusListObject();
        response.setResponseStatusObject(Arrays.asList(statusObjects));
        object.setResponseStatusListObject(response);
        return object;
    }

    public static VIIDResponseStatusObject from(String requestUrl, List<ResponseStatusObject> statusObjects) {
        VIIDResponseStatusObject object = new VIIDResponseStatusObject();
        ResponseStatusListObject response = new ResponseStatusListObject();
        for (ResponseStatusObject statusObject : statusObjects) {
            if (requestUrl != null) {
                statusObject.setRequestUrl(requestUrl);
            }
        }
        if (statusObjects.isEmpty()) {
            response.setResponseStatusObject(
                    Collections.singletonList(
                            new ResponseStatusObject(requestUrl, "1", "不存在请求数据")
                    )
            );
        } else {
            response.setResponseStatusObject(statusObjects);
        }
        object.setResponseStatusListObject(response);
        return object;
    }
}
