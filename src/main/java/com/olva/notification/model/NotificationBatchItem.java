package com.olva.notification.model;

import java.time.LocalDateTime;
import java.util.List;

public record NotificationBatchItem(
        Long id,
        String clientId,
        Long shipmentId,
        String orderNumber,
        String trackingNumber,
        String carrierTrackingNumber,
        String carrierName,
        String carrierCode,
        String cudNumber,
        String pieceNumber,
        Long eventId,
        Long stateId,
        Long rptEnvioRutaId,
        String eventCode,
        String eventDescription,
        Long homologationId,
        String emision,
        Long remito,
        String glosa,
        int attemptCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime deadlineDate,
        LocalDateTime orderCreatedAt,
        List<String> cudNumbers,
        List<String> trackingUrls
) {
}
