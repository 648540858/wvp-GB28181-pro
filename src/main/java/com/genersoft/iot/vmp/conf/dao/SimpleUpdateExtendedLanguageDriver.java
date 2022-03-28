package com.genersoft.iot.vmp.conf.dao;

import com.google.common.base.CaseFormat;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.scripting.LanguageDriver;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import org.apache.ibatis.session.Configuration;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * mybatis 注解模式 update 支持传入对象 驱动
 * UPDATE xxx_user (#{userObject}) WHERE true and xxxx
 *
 * @author
 */
public class SimpleUpdateExtendedLanguageDriver extends XMLLanguageDriver implements LanguageDriver {
	private final Pattern inPattern = Pattern.compile("\\(#\\{(\\w+)\\}\\)");

	@Override
	public SqlSource createSqlSource(Configuration configuration, String script, Class<?> parameterType) {
		Matcher matcher = inPattern.matcher(script);
		if (matcher.find()) {
			StringBuffer ss = new StringBuffer();
			ss.append("<set>");

			for (Field field : parameterType.getDeclaredFields()) {
				if (!field.isAnnotationPresent(Id.class)){
					String temp = "";
					if(field.getType()== Date.class){
						temp = "<if test=\"__field != null \">__column=#{__field},</if>";
					}else {
						temp = "<if test=\"__field != null \">__column=#{__field},</if>";
					}
					ss.append(temp.replaceAll("__field", field.getName()).replaceAll("__column",
							CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, field.getName())));
				}

			}

			ss.deleteCharAt(ss.lastIndexOf(","));
			ss.append("</set>");

			script = matcher.replaceAll(ss.toString());

			script = "<script>" + script + "</script>";
		}
		return super.createSqlSource(configuration, script, parameterType);
	}
}
