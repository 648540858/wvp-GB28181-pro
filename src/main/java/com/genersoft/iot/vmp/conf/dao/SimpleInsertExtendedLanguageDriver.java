package com.genersoft.iot.vmp.conf.dao;

import com.google.common.base.CaseFormat;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.scripting.LanguageDriver;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import org.apache.ibatis.session.Configuration;

import java.lang.reflect.Field;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * mybatis 注解模式 insert 支持传入对象 驱动
 * INSERT INTO xxx_user (#{userObject})
 *
 * @author
 */
public class SimpleInsertExtendedLanguageDriver extends XMLLanguageDriver implements LanguageDriver {
    private final Pattern inPattern = Pattern.compile("\\(#\\{(\\w+)\\}\\)");

    @Override
    public SqlSource createSqlSource(Configuration configuration, String script, Class<?> parameterType) {
        Matcher matcher = inPattern.matcher(script);
        if (matcher.find()) {

            // 组建 (xx, xx , xx)字段语句
            StringBuffer ss = new StringBuffer("( <trim suffixOverrides=\",\">");
            for (Field field : parameterType.getDeclaredFields()) {
                //如果不是加了忽略注解的字段就去拼接
                if (field.isAnnotationPresent(Invisible.class)) {
                    continue;
                }

                String column = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, field.getName());
                String temp = "<if test=\"__field != null\"> __column,</if>";
                ss.append(temp.replaceAll("__field", field.getName()).replaceAll("__column", column));
            }
            ss.append("</trim>) VALUES ( <trim suffixOverrides=\",\">");

            // 组建 ("1", "xx"， "")值语句
            for (Field field : parameterType.getDeclaredFields()) {
                if (field.isAnnotationPresent(Invisible.class)) {
                    continue;
                }

                String temp = "<if test=\"__field != null\"> #{__field},</if>";
                ss.append(temp.replaceAll("__field", field.getName()));
            }

            ss.append("</trim>) ");
            script = matcher.replaceAll(ss.toString());

            script = "<script>" + script + "</script>";
        }
        return super.createSqlSource(configuration, script, parameterType);
    }

}
