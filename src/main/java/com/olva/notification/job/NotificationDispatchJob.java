package com.olva.notification.job;

import com.olva.notification.service.NotificationDispatchService;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
@DisallowConcurrentExecution
public class NotificationDispatchJob extends QuartzJobBean {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatchJob.class);
    private static final AtomicBoolean RUNNING = new AtomicBoolean(false);

    private final NotificationDispatchService notificationDispatchService;

    public NotificationDispatchJob(NotificationDispatchService notificationDispatchService) {
        this.notificationDispatchService = notificationDispatchService;
    }

    @Override
    protected void executeInternal(JobExecutionContext context) {
        String jobName = resolveJobName(context);

        if (!RUNNING.compareAndSet(false, true)) {
            log.warn("event=notification_dispatch_job_skipped job={} reason=already_running triggerTime={}",
                    jobName,
                    context != null ? context.getFireTime() : null);
            return;
        }

        log.info("event=notification_dispatch_job_started job={} triggerTime={}",
                jobName,
                context != null ? context.getFireTime() : null);

        try {
            notificationDispatchService.dispatchAll();
        } finally {
            RUNNING.set(false);
        }

        log.info("event=notification_dispatch_job_finished job={} nextFireTime={}",
                jobName,
                context != null ? context.getNextFireTime() : null);
    }

    private String resolveJobName(JobExecutionContext context) {
        if (context == null) {
            return "notificationDispatchJob";
        }

        JobDetail jobDetail = context.getJobDetail();
        return jobDetail != null ? jobDetail.getKey().toString() : "notificationDispatchJob";
    }
}
