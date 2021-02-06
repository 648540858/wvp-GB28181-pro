package com.genersoft.iot.vmp.conf;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**    
 * @Description: 获取数据库配置   
 * @author: swwheihei
 * @date:   2020年5月6日 下午2:46:00     
 */
@Configuration("vmConfig")
public class VManagerConfig {

	@Value("${spring.application.database:redis}")
    private String database;


    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }
}
