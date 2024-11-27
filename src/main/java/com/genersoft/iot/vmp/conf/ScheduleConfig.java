package com.genersoft.iot.vmp.conf;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Component;

/**
 * "@Scheduled"是Spring框架提供的一种定时任务执行机制，默认情况下它是单线程的，在同时执行多个定时任务时可能会出现阻塞和性能问题。
 * 为了解决这种单线程瓶颈问题，可以将定时任务的执行机制改为支持多线程
 */
@Component
public class ScheduleConfig implements SchedulingConfigurer {

    @Qualifier("taskExecutor")
	private ThreadPoolTaskExecutor taskExecutor;


	@Override
	public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
		taskRegistrar.setScheduler(taskExecutor);
	}
}
