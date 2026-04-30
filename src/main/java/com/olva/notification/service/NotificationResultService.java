package com.olva.notification.service;

import com.olva.notification.model.NotificationAttemptDetail;
import com.olva.notification.model.NotificationAttemptRecord;
import com.olva.notification.model.NotificationBatchItem;
import com.olva.notification.model.NotificationProcessingResult;
import com.olva.notification.repository.NotificationAttemptRepository;
import com.olva.notification.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class NotificationResultService {

    private static final Logger log = LoggerFactory.getLogger(NotificationResultService.class);

    private final NotificationRepository notificationRepository;
    private final NotificationAttemptRepository notificationAttemptRepository;

    public NotificationResultService(NotificationRepository notificationRepository,
                                     NotificationAttemptRepository notificationAttemptRepository) {
        this.notificationRepository = notificationRepository;
        this.notificationAttemptRepository = notificationAttemptRepository;
    }

    public void registerResult(NotificationBatchItem notification,
                               NotificationProcessingResult result) {
        int nextAttemptCount = notification.attemptCount() + 1;
        LocalDateTime now = LocalDateTime.now();

        for (NotificationAttemptDetail attemptDetail : resolveAttemptDetails(result)) {
            notificationAttemptRepository.save(new NotificationAttemptRecord(
                    notification.id(),
                    notification.shipmentId(),
                    notification.eventId(),
                    notification.emision(),
                    notification.remito(),
                    nextAttemptCount,
                    attemptDetail.status(),
                    attemptDetail.requestPayload(),
                    attemptDetail.responseCode(),
                    attemptDetail.responseBody(),
                    attemptDetail.message(),
                    now
            ));
        }

        if (isDiscarded(result)) {
            notificationRepository.markAsDiscarded(
                    notification.id(),
                    nextAttemptCount,
                    result.requestPayload(),
                    result.responseBody(),
                    result.errorCode(),
                    result.errorMessage(),
                    now
            );
            log.warn("event=notification_discarded notificationId={} shipmentId={} attemptNumber={} errorCode={} errorMessage={}",
                    notification.id(),
                    notification.shipmentId(),
                    nextAttemptCount,
                    result.errorCode(),
                    result.errorMessage());
            return;
        }

        if (result.successful()) {
            notificationRepository.markAsSent(
                    notification.id(),
                    nextAttemptCount,
                    result.httpStatus(),
                    result.responseBody(),
                    result.requestPayload(),
                    now
            );
            log.info("event=notification_result_saved notificationId={} shipmentId={} finalStatus=SENT attemptNumber={} httpStatus={}",
                    notification.id(),
                    notification.shipmentId(),
                    nextAttemptCount,
                    result.httpStatus());
            return;
        }

        if (result.retryable()) {
            notificationRepository.markAsRetry(
                    notification.id(),
                    nextAttemptCount,
                    result.httpStatus(),
                    result.responseBody(),
                    result.requestPayload(),
                    result.errorCode(),
                    result.errorMessage()
            );
            log.warn("event=notification_result_saved notificationId={} shipmentId={} finalStatus=RETRY attemptNumber={} errorCode={} httpStatus={}",
                    notification.id(),
                    notification.shipmentId(),
                    nextAttemptCount,
                    result.errorCode(),
                    result.httpStatus());
            return;
        }

        notificationRepository.markAsFailed(
                notification.id(),
                nextAttemptCount,
                result.httpStatus(),
                result.responseBody(),
                result.requestPayload(),
                result.errorCode(),
                result.errorMessage(),
                now
        );
        log.error("event=notification_result_saved notificationId={} shipmentId={} finalStatus=FAILED attemptNumber={} errorCode={} httpStatus={}",
                notification.id(),
                notification.shipmentId(),
                nextAttemptCount,
                result.errorCode(),
                result.httpStatus());
    }

    private boolean isDiscarded(NotificationProcessingResult result) {
        return !result.successful()
                && !result.retryable()
                && "DISCARDED".equalsIgnoreCase(result.errorCode());
    }

    private Iterable<NotificationAttemptDetail> resolveAttemptDetails(NotificationProcessingResult result) {
        if (result.attemptDetails() != null && !result.attemptDetails().isEmpty()) {
            return result.attemptDetails();
        }

        return java.util.List.of(new NotificationAttemptDetail(
                null,
                resolveAttemptStatus(result),
                result.requestPayload(),
                resolveResponseCode(result),
                result.responseBody(),
                resolveMessage(result)
        ));
    }

    private String resolveAttemptStatus(NotificationProcessingResult result) {
        if (result.successful()) {
            return "SUCCESS";
        }
        if (isDiscarded(result)) {
            return "DISCARDED";
        }
        return result.retryable() ? "RETRY" : "FAILED";
    }

    private String resolveResponseCode(NotificationProcessingResult result) {
        if (result.httpStatus() != null) {
            return String.valueOf(result.httpStatus());
        }
        return result.errorCode();
    }

    private String resolveMessage(NotificationProcessingResult result) {
        if (result.successful()) {
            return "Notificacion enviada correctamente";
        }
        return result.errorMessage();
    }
}
