package com.genersoft.iot.vmp.utils;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**    
 * @description:spring bean获取工厂，获取spring中的已初始化的bean
 * @author: swwheihei
 * @date:   2019年6月25日 下午4:51:52   
 * 
 */
@Component
public class SpringBeanFactory implements ApplicationContextAware {

	// Spring应用上下文环境
    private static ApplicationContext applicationContext;
    
    /**
     * 实现ApplicationContextAware接口的回调方法，设置上下文环境
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext)
            throws BeansException {
    	SpringBeanFactory.applicationContext = applicationContext;
    }

    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    /**
     * 获取对象 这里重写了bean方法，起主要作用
     */
    public static Object getBean(String beanId) throws BeansException {
        if (applicationContext == null) {
            return null;
        }
        return applicationContext.getBean(beanId);
    }

    /**
     * 获取当前环境
     */
    public static String getActiveProfile() {
        return applicationContext.getEnvironment().getActiveProfiles()[0];
    }

}
