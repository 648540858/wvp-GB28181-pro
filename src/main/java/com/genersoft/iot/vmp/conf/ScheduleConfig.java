package com.genersoft.iot.vmp.conf;

import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * "@Scheduled"是Spring框架提供的一种定时任务执行机制，默认情况下它是单线程的，在同时执行多个定时任务时可能会出现阻塞和性能问题。
 * 为了解决这种单线程瓶颈问题，可以将定时任务的执行机制改为支持多线程
 */
@Configuration
public class ScheduleConfig implements SchedulingConfigurer {

	public static final int cpuNum = Runtime.getRuntime().availableProcessors();

	private static final int corePoolSize = cpuNum;

	private static final String threadNamePrefix = "scheduled-task-pool-%d";

	@Override
	public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
		taskRegistrar.setScheduler(new ScheduledThreadPoolExecutor(corePoolSize,
				new BasicThreadFactory.Builder().namingPattern(threadNamePrefix).daemon(true).build(),
				new ThreadPoolExecutor.CallerRunsPolicy()));
	}
}
