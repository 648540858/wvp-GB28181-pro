package com.genersoft.iot.vmp.service.bean;

import lombok.Getter;
import lombok.Setter;

import java.io.File;

/**
 * 配置文件条目
 * <p>
 * 用于统一描述外部配置文件与 classpath(jar内) 中的配置文件
 */
@Setter
@Getter
public class ConfigEntry {

    /**
     * 配置文件ID, 即 application-{id}.yml 中的 {id}
     */
    private String id;

    /**
     * 文件名, 如 application-dev.yml
     */
    private String fileName;

    /**
     * 文件路径描述, 外部文件为绝对路径, jar内为 classpath 地址
     */
    private String path;

    /**
     * 外部真实文件, jar内配置无法解析为文件时为 null
     */
    private File file;

    /**
     * 配置内容
     */
    private byte[] content;

    /**
     * 是否来自 classpath(jar内)
     */
    private boolean classpathResource;

    /**
     * 是否可写(可修改其中的 spring.profiles.active)
     */
    public boolean isWritable() {
        return file != null && file.isFile() && file.canWrite();
    }
}
