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
 * mybatis 注解模式 select 支持传入对象 驱动
 * SELECT * FROM xxx_user where 1=1 (#{userObject})
 *
 * @author
 */
public class SelectByEntityExtendedLanguageDriver extends XMLLanguageDriver implements LanguageDriver {

	private final Pattern inPattern = Pattern.compile("\\(#\\{(\\w+)\\}\\)");

	@Override
	public SqlSource createSqlSource(Configuration configuration, String script, Class<?> parameterType) {

		Matcher matcher = inPattern.matcher(script);
		if (matcher.find()) {
			StringBuffer ss = new StringBuffer();
			for (Field field : parameterType.getDeclaredFields()) {
				//如果不是加了忽略注解的字段就去拼接
				if (!field.isAnnotationPresent(Invisible.class)) {
					//and type !=''
				String temp = "<if test=\"__field != null and __field!='' \">and __column=#{__field}</if>";
				ss.append(temp.replaceAll("__field", field.getName()).replaceAll("__column",
						CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, field.getName())));
				}
			}
			script = matcher.replaceAll(ss.toString());
			script = "<script>" + script + "</script>";
		}
		return super.createSqlSource(configuration, script, parameterType);
	}
}
