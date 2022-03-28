package com.genersoft.iot.vmp.conf.dao;

import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.scripting.LanguageDriver;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import org.apache.ibatis.session.Configuration;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * mybatis 注解模式 select 支持传入list 驱动
 * SELECT * FROM xxx_user WHERE user_id IN (#{userIds})
 *
 * @author
 */
public class SimpleSelectInExtendedLanguageDriver extends XMLLanguageDriver implements LanguageDriver {

	private final Pattern inPattern = Pattern.compile("\\(#\\{(\\w+)\\}\\)");

	@Override
	public SqlSource createSqlSource(Configuration configuration, String script, Class<?> parameterType) {

		Matcher matcher = inPattern.matcher(script);
		if (matcher.find()) {
			script = matcher
					.replaceAll("(<foreach collection=\"$1\" item=\"__item\" separator=\",\" >#{__item}</foreach>)");
		}

		script = "<script>" + script + "</script>";
		return super.createSqlSource(configuration, script, parameterType);
	}
}
