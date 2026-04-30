package com.olva.notification.model;

public record NotificationAttemptDetail(
        String cudNumber,
        String status,
        String requestPayload,
        String responseCode,
        String responseBody,
        String message
) {
}
