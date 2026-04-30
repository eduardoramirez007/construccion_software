package com.olva.notification.model;

import java.util.List;

public record NotificationProcessingResult(
        Long notificationId,
        boolean successful,
        boolean retryable,
        Integer httpStatus,
        String requestPayload,
        String responseBody,
        String errorCode,
        String errorMessage,
        List<NotificationAttemptDetail> attemptDetails
) {

    public static NotificationProcessingResult success(Long notificationId,
                                                       Integer httpStatus,
                                                       String requestPayload,
                                                       String responseBody) {
        return new NotificationProcessingResult(
                notificationId,
                true,
                false,
                httpStatus,
                requestPayload,
                responseBody,
                null,
                null,
                List.of(new NotificationAttemptDetail(
                        null,
                        "SUCCESS",
                        requestPayload,
                        httpStatus != null ? String.valueOf(httpStatus) : null,
                        responseBody,
                        "Notificacion enviada correctamente"
                ))
        );
    }

    public static NotificationProcessingResult success(Long notificationId,
                                                       Integer httpStatus,
                                                       String requestPayload,
                                                       String responseBody,
                                                       List<NotificationAttemptDetail> attemptDetails) {
        return new NotificationProcessingResult(
                notificationId,
                true,
                false,
                httpStatus,
                requestPayload,
                responseBody,
                null,
                null,
                attemptDetails
        );
    }

    public static NotificationProcessingResult retryableError(Long notificationId,
                                                              Integer httpStatus,
                                                              String requestPayload,
                                                              String responseBody,
                                                              String errorCode,
                                                              String errorMessage) {
        return new NotificationProcessingResult(
                notificationId,
                false,
                true,
                httpStatus,
                requestPayload,
                responseBody,
                errorCode,
                errorMessage,
                List.of(new NotificationAttemptDetail(
                        null,
                        "RETRY",
                        requestPayload,
                        httpStatus != null ? String.valueOf(httpStatus) : errorCode,
                        responseBody,
                        errorMessage
                ))
        );
    }

    public static NotificationProcessingResult retryableError(Long notificationId,
                                                              Integer httpStatus,
                                                              String requestPayload,
                                                              String responseBody,
                                                              String errorCode,
                                                              String errorMessage,
                                                              List<NotificationAttemptDetail> attemptDetails) {
        return new NotificationProcessingResult(
                notificationId,
                false,
                true,
                httpStatus,
                requestPayload,
                responseBody,
                errorCode,
                errorMessage,
                attemptDetails
        );
    }

    public static NotificationProcessingResult nonRetryableError(Long notificationId,
                                                                 Integer httpStatus,
                                                                 String requestPayload,
                                                                 String responseBody,
                                                                 String errorCode,
                                                                 String errorMessage) {
        return new NotificationProcessingResult(
                notificationId,
                false,
                false,
                httpStatus,
                requestPayload,
                responseBody,
                errorCode,
                errorMessage,
                List.of(new NotificationAttemptDetail(
                        null,
                        "DISCARDED",
                        requestPayload,
                        httpStatus != null ? String.valueOf(httpStatus) : errorCode,
                        responseBody,
                        errorMessage
                ))
        );
    }
}
