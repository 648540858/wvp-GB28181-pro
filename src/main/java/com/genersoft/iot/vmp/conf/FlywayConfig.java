package com.genersoft.iot.vmp.conf;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

/**
 * 数据库自动升级脚本
 */
@Configuration
public class FlywayConfig {

    private final static Logger logger = LoggerFactory.getLogger(FlywayConfig.class);

    @Autowired
    private DataSource dataSource;

    @PostConstruct
    public void migrate() {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("db/migration")//sql文件名称规则："V20210625.17.30__V1.0.sql"
                // 自动创建flyway元数据表标识
                .baselineOnMigrate(true)
                .baselineVersion("0")
                // 禁止flyway执行清理
                .cleanDisabled(true)
                .table("flyway_schema_history_wvp")
                // 对于开发环境, 可能是多人协作开发, 很可能先 apply 了自己本地的最新 SQL 代码, 然后发现其他同事早先时候提交的 SQL 代码还没有 apply,
                // 所以 开发环境应该设置 spring.flyway.outOfOrder=true, 这样 flyway 将能加载漏掉的老版本 SQL 文件;
                // 而生产环境应该设置 spring.flyway.outOfOrder=false
                .outOfOrder(true)
                .encoding("UTF-8")
                .load();
        try {
            flyway.migrate();
        } catch (FlywayException e) {
            logger.error("[数据库自动升级]配置第一次加载出错", e);
            try {
                flyway.repair();//生成版本记录表
                logger.info("[数据库自动升级]配置修复成功");
                flyway.migrate();
                logger.info("[数据库自动升级]配置重新加载成功");
            } catch (Exception e1) {
                logger.error("[数据库自动升级]配置第二次加载出错", e1);
                throw e1;
            }
        }
    }

}
