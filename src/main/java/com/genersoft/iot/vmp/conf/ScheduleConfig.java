package com.genersoft.iot.vmp.conf;

import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;

import static com.genersoft.iot.vmp.conf.ThreadPoolTaskConfig.cpuNum;

/**
 * "@Scheduled"是Spring框架提供的一种定时任务执行机制，默认情况下它是单线程的，在同时执行多个定时任务时可能会出现阻塞和性能问题。
 * 为了解决这种单线程瓶颈问题，可以将定时任务的执行机制改为支持多线程
 */
@Configuration
public class ScheduleConfig implements SchedulingConfigurer {

	/**
	 * 核心线程数（默认线程数）
	 */
	private static final int corePoolSize = Math.max(cpuNum, 20);

	/**
	 * 线程池名前缀
	 */
	private static final String threadNamePrefix = "schedule";

	@Override
	public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
		ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(corePoolSize,
				new BasicThreadFactory.Builder().namingPattern(threadNamePrefix).daemon(true).build(),
				new ThreadPoolExecutor.CallerRunsPolicy());
		taskRegistrar.setScheduler(scheduledThreadPoolExecutor);
	}
}
