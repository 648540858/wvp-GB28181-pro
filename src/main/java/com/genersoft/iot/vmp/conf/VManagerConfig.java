package com.genersoft.iot.vmp.conf;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**    
 * @Description:TODO(这里用一句话描述这个类的作用)   
 * @author: swwheihei
 * @date:   2020年5月6日 下午2:46:00     
 */
@Data
@Configuration("vmConfig")
public class VManagerConfig {

	@Value("${spring.application.database:redis}")
    private String database;


}
