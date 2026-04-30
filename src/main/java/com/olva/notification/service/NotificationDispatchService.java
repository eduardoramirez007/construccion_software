package com.olva.notification.service;

import com.olva.notification.gateway.ClientNotificationGateway;
import com.olva.notification.model.NotificationBatchItem;
import com.olva.notification.model.NotificationProcessingResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class NotificationDispatchService {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatchService.class);

    private final List<ClientNotificationGateway> gateways;
    private final NotificationResultService notificationResultService;

    public NotificationDispatchService(List<ClientNotificationGateway> gateways,
                                       NotificationResultService notificationResultService) {
        this.gateways = gateways;
        this.notificationResultService = notificationResultService;
    }

    public void dispatchAll() {
        log.info("event=notification_dispatch_all_started gateways={}", gateways.size());

        for (ClientNotificationGateway gateway : gateways) {
            dispatch(gateway);
        }

        log.info("event=notification_dispatch_all_finished gateways={}", gateways.size());
    }

    public void dispatch(String clientId) {
        gateways.stream()
                .filter(gateway -> gateway.clientId().equalsIgnoreCase(clientId))
                .findFirst()
                .ifPresent(this::dispatch);
    }

    private void dispatch(ClientNotificationGateway gateway) {
        List<NotificationBatchItem> notifications = gateway.fetchPendingBatch();

        log.info("event=notification_batch_claimed clientId={} claimedCount={}",
                gateway.clientId(),
                notifications.size());

        if (!gateway.groupByShipment()) {
            log.info("event=notification_batch_processing_mode clientId={} mode=ONE_BY_ONE itemCount={}",
                    gateway.clientId(),
                    notifications.size());
            processNotificationsOneByOne(gateway, notifications);
            return;
        }

        Map<Long, List<NotificationBatchItem>> notificationsByShipment = notifications.stream()
                .collect(Collectors.groupingBy(
                        NotificationBatchItem::shipmentId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        for (List<NotificationBatchItem> shipmentNotifications : notificationsByShipment.values()) {
            shipmentNotifications.sort(Comparator.comparing(NotificationBatchItem::createdAt));
            log.info("event=notification_shipment_group_ready clientId={} shipmentId={} itemCount={}",
                    gateway.clientId(),
                    shipmentNotifications.getFirst().shipmentId(),
                    shipmentNotifications.size());
            processShipmentNotifications(gateway, shipmentNotifications);
        }
    }

    private void processNotificationsOneByOne(ClientNotificationGateway gateway,
                                              List<NotificationBatchItem> notifications) {
        for (NotificationBatchItem notification : notifications) {
            processNotification(gateway, notification);
        }
    }

    private void processShipmentNotifications(ClientNotificationGateway gateway,
                                              List<NotificationBatchItem> notifications) {
        for (NotificationBatchItem notification : notifications) {
            NotificationProcessingResult result = processNotification(gateway, notification);

            if (!result.successful()) {
                log.warn("event=notification_shipment_sequence_stopped clientId={} shipmentId={} failedNotificationId={} retryable={} errorCode={}",
                        gateway.clientId(),
                        notification.shipmentId(),
                        notification.id(),
                        result.retryable(),
                        result.errorCode());
                break;
            }
        }
    }

    private NotificationProcessingResult processNotification(ClientNotificationGateway gateway,
                                                             NotificationBatchItem notification) {
        log.info("event=notification_processing_started clientId={} notificationId={} shipmentId={} attemptCount={} eventCode={} subEventCode={}",
                gateway.clientId(),
                notification.id(),
                notification.shipmentId(),
                notification.attemptCount(),
                notification.eventCode(),
                notification.eventDescription());

        NotificationProcessingResult result = gateway.process(notification);
        notificationResultService.registerResult(notification, result);

        log.info("event=notification_processing_finished clientId={} notificationId={} shipmentId={} successful={} retryable={} httpStatus={} errorCode={}",
                gateway.clientId(),
                notification.id(),
                notification.shipmentId(),
                result.successful(),
                result.retryable(),
                result.httpStatus(),
                result.errorCode());

        return result;
    }
}
