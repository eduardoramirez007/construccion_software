package com.olva.notification.repository;

import com.olva.notification.model.NotificationBatchItem;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository {

    List<NotificationBatchItem> claimPendingByPersonId(Long personId, String clientId, int batchSize, int maxAttempts);

    List<NotificationBatchItem> claimPendingSerhafen(String clientId, int batchSize, int maxAttempts);

    void markAsSent(Long notificationId,
                    int nextAttemptCount,
                    Integer httpStatus,
                    String responseBody,
                    String requestPayload,
                    LocalDateTime processedAt);

    void markAsRetry(Long notificationId,
                     int nextAttemptCount,
                     Integer httpStatus,
                     String responseBody,
                     String requestPayload,
                     String errorCode,
                     String errorMessage);

    void markAsFailed(Long notificationId,
                      int nextAttemptCount,
                      Integer httpStatus,
                      String responseBody,
                      String requestPayload,
                      String errorCode,
                      String errorMessage,
                      LocalDateTime processedAt);

    void markAsDiscarded(Long notificationId,
                         int nextAttemptCount,
                         String requestPayload,
                         String responseBody,
                         String errorCode,
                         String errorMessage,
                         LocalDateTime processedAt);
}
