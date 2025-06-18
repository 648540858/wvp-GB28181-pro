package com.genersoft.iot.vmp.gat1400.framework;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

/**
 * 作者：Administrator
 * 创建时间：2025/1/17
 * 邮箱：yongwangyi999@163.com
 */
public class StatusCodeDeSerializer extends JsonDeserializer<String> {
    @Override
    public String deserialize(JsonParser parser, DeserializationContext context) throws IOException, JacksonException {
        JsonToken token = parser.getCurrentToken();
        if (token == JsonToken.VALUE_STRING) {
            return parser.getText();
        } else if (token == JsonToken.VALUE_NUMBER_INT) {
            return String.valueOf(parser.getIntValue());
        }
        return null;
    }
}
