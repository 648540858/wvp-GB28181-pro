package com.genersoft.iot.vmp.conf;

import org.apache.ibatis.logging.stdout.StdOutImpl;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import javax.sql.DataSource;

/**
 * 配置mybatis
 */
@Configuration
@Order(value=1)
public class MybatisConfig {

    @Autowired
    private UserSetting userSetting;

    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
       final SqlSessionFactoryBean sqlSessionFactory = new SqlSessionFactoryBean();
        sqlSessionFactory.setDataSource(dataSource);
        org.apache.ibatis.session.Configuration config = new org.apache.ibatis.session.Configuration();
        if (userSetting.getSqlLog()){
            config.setLogImpl(StdOutImpl.class);
        }
        config.setMapUnderscoreToCamelCase(true);
        sqlSessionFactory.setConfiguration(config);
        return sqlSessionFactory.getObject();
    }

}
