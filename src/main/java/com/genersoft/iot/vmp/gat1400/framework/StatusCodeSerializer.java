package com.genersoft.iot.vmp.gat1400.framework;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.io.IOException;

/**
 * 作者：Administrator
 * 创建时间：2025/1/17
 * 邮箱：yongwangyi999@163.com
 */
public class StatusCodeSerializer extends JsonSerializer<String> {

    @Override
    public void serialize(String value, JsonGenerator generator, SerializerProvider provider) throws IOException {
        if (StringUtils.isBlank(value))
            generator.writeNull();
       generator.writeNumber(NumberUtils.toInt(value));
    }
}
