package com.genersoft.iot.vmp.gat1400.fontend.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DataSelectOption {

    private String id;
    private String label;
    private String value;
    private Object data;

    public static DataSelectOption from(String value, String label) {
        return from(value, label, null);
    }

    public static DataSelectOption from(String value, String label, Object data) {
        return new DataSelectOption(value, label, value, data);
    }
}
