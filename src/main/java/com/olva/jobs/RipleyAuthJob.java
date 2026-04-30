package com.olva.jobs;

import com.olva.service.RipleyAuthService;
import org.quartz.JobExecutionContext;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

@Component
public class RipleyAuthJob extends QuartzJobBean {

    private final RipleyAuthService ripleyAuthService;

    public RipleyAuthJob(RipleyAuthService ripleyAuthService) {
        this.ripleyAuthService = ripleyAuthService;
    }

    @Override
    protected void executeInternal(JobExecutionContext context) {
        ripleyAuthService.refreshToken();

    }
}
