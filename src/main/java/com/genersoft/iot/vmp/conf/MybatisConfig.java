package com.genersoft.iot.vmp.conf;

import org.apache.ibatis.logging.stdout.StdOutImpl;
import org.apache.ibatis.mapping.DatabaseIdProvider;
import org.apache.ibatis.mapping.VendorDatabaseIdProvider;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import javax.sql.DataSource;
import java.util.Properties;

/**
 * 配置mybatis
 */
@Configuration
@Order(value=1)
public class MybatisConfig {

    @Autowired
    private UserSetting userSetting;

    @Bean
    public DatabaseIdProvider databaseIdProvider() {
        VendorDatabaseIdProvider databaseIdProvider = new VendorDatabaseIdProvider();
        Properties properties = new Properties();
        properties.setProperty("Oracle", "oracle");
        properties.setProperty("MySQL", "mysql");
        properties.setProperty("DB2", "db2");
        properties.setProperty("Derby", "derby");
        properties.setProperty("H2", "h2");
        properties.setProperty("HSQL", "hsql");
        properties.setProperty("Informix", "informix");
        properties.setProperty("MS-SQL", "ms-sql");
        properties.setProperty("PostgreSQL", "postgresql");
        properties.setProperty("Sybase", "sybase");
        properties.setProperty("Hana", "hana");
        properties.setProperty("DM", "dm");
        properties.setProperty("KingbaseES", "kingbase");
        properties.setProperty("KingBase8", "kingbase");
        databaseIdProvider.setProperties(properties);
        return databaseIdProvider;
    }

    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource, DatabaseIdProvider databaseIdProvider) throws Exception {
       final SqlSessionFactoryBean sqlSessionFactory = new SqlSessionFactoryBean();
        sqlSessionFactory.setDataSource(dataSource);
        org.apache.ibatis.session.Configuration config = new org.apache.ibatis.session.Configuration();
        if (userSetting.getSqlLog()){
            config.setLogImpl(StdOutImpl.class);
        }
        config.setMapUnderscoreToCamelCase(true);
        sqlSessionFactory.setConfiguration(config);
        sqlSessionFactory.setDatabaseIdProvider(databaseIdProvider);
        return sqlSessionFactory.getObject();
    }

}
