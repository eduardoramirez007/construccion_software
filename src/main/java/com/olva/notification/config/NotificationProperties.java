package com.olva.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "integration.notification")
public class NotificationProperties {

    private int batchSize;
    private int maxAttempts;
    private Long pendingStateId;
    private Long deliveredStateId;

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public Long getPendingStateId() {
        return pendingStateId;
    }

    public void setPendingStateId(Long pendingStateId) {
        this.pendingStateId = pendingStateId;
    }

    public Long getDeliveredStateId() {
        return deliveredStateId;
    }

    public void setDeliveredStateId(Long deliveredStateId) {
        this.deliveredStateId = deliveredStateId;
    }
}
