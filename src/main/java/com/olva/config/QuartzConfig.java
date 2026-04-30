package com.olva.config;

import com.olva.jobs.RipleyAuthJob;
import com.olva.jobs.RipleyOrdersJob;
import com.olva.notification.job.NotificationDispatchJob;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuartzConfig {

    @Value("${integration.ripley.token.cron}")
    private String tokenCron;

    @Value("${integration.ripley.orders.cron}")
    private String ordersCron;

    @Value("${integration.notification.dispatch.cron}")
    private String notificationDispatchCron;

    @Bean
    public JobDetail ripleyAuthJobDetail() {
        return JobBuilder.newJob(RipleyAuthJob.class)
                .withIdentity("ripleyAuthJob")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger ripleyAuthTrigger() {
        return TriggerBuilder.newTrigger()
                .forJob(ripleyAuthJobDetail())
                .withIdentity("ripleyAuthTrigger")
                .withSchedule(CronScheduleBuilder.cronSchedule(tokenCron))
                .build();
    }

    @Bean
    public JobDetail ripleyOrdersJobDetail() {
        return JobBuilder.newJob(RipleyOrdersJob.class)
                .withIdentity("ripleyOrdersJob")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger ripleyOrdersTrigger() {
        return TriggerBuilder.newTrigger()
                .forJob(ripleyOrdersJobDetail())
                .withIdentity("ripleyOrdersTrigger")
                .withSchedule(CronScheduleBuilder.cronSchedule(ordersCron))
                .build();
    }

    @Bean
    public JobDetail notificationDispatchJobDetail() {
        return JobBuilder.newJob(NotificationDispatchJob.class)
                .withIdentity("notificationDispatchJob")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger notificationDispatchTrigger() {
        return TriggerBuilder.newTrigger()
                .forJob(notificationDispatchJobDetail())
                .withIdentity("notificationDispatchTrigger")
                .withSchedule(CronScheduleBuilder.cronSchedule(notificationDispatchCron))
                .build();
    }
}
