package com.genersoft.iot.vmp.utils;

import java.util.Arrays;

public class CollectionUtil {

    public static <T> boolean contains(T[] array, final T element) {
        return array != null && Arrays.stream(array).anyMatch((x) -> {
            return ObjectUtils.nullSafeEquals(x, element);
        });
    }
}
