package com.olva.notification.model;

import java.time.LocalDateTime;

public record NotificationAttemptRecord(
        Long outboxId,
        Long shipmentId,
        Long eventId,
        String emision,
        Long remito,
        int attemptNumber,
        String status,
        String requestPayload,
        String responseCode,
        String responseBody,
        String message,
        LocalDateTime attemptedAt
) {
}
