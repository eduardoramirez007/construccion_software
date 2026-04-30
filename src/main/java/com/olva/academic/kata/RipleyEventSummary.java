package com.olva.academic.kata;

import java.util.List;

public record RipleyEventSummary(
        Long notificationId,
        Long shipmentId,
        String eventCode,
        List<String> normalizedCuds
) {
}
