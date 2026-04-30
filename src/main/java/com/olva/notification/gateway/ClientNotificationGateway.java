package com.olva.notification.gateway;

import com.olva.notification.model.NotificationBatchItem;
import com.olva.notification.model.NotificationProcessingResult;

import java.util.List;

public interface ClientNotificationGateway {

    String clientId();

    List<NotificationBatchItem> fetchPendingBatch();

    NotificationProcessingResult process(NotificationBatchItem notification);

    default boolean groupByShipment() {
        return true;
    }
}
