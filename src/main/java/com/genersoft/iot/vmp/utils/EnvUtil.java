package com.genersoft.iot.vmp.utils;

public class EnvUtil {

    public static boolean isDockerEnv() {
        return "docker".equals(System.getenv("RUN_ENV"));
    }
}
