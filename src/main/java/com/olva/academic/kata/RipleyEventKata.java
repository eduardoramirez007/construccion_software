package com.olva.academic.kata;

import com.olva.notification.model.NotificationBatchItem;

import java.util.LinkedHashSet;
import java.util.List;

public class RipleyEventKata {

    public RipleyEventSummary summarize(NotificationBatchItem item) {
        validateRequired(item);
        return new RipleyEventSummary(
                item.id(),
                item.shipmentId(),
                item.eventCode().trim(),
                normalizeCuds(item.cudNumbers())
        );
    }

    private void validateRequired(NotificationBatchItem item) {
        if (item == null) {
            throw new IllegalArgumentException("La notificacion es obligatoria");
        }
        if (item.id() == null || item.id() <= 0) {
            throw new IllegalArgumentException("El id de notificacion debe ser positivo");
        }
        if (item.shipmentId() == null || item.shipmentId() <= 0) {
            throw new IllegalArgumentException("El envio debe ser positivo");
        }
        if (item.eventCode() == null || item.eventCode().isBlank()) {
            throw new IllegalArgumentException("El codigo de evento Ripley es obligatorio");
        }
    }

    private List<String> normalizeCuds(List<String> cuds) {
        if (cuds == null || cuds.isEmpty()) {
            return List.of();
        }

        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String cud : cuds) {
            if (cud != null && !cud.isBlank()) {
                normalized.add(cud.trim().toUpperCase());
            }
        }
        return List.copyOf(normalized);
    }
}
