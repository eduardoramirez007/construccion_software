package com.olva.notification.service;

import com.olva.notification.gateway.ClientNotificationGateway;
import com.olva.notification.model.NotificationBatchItem;
import com.olva.notification.model.NotificationProcessingResult;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationDispatchServiceTest {

    @Test
    void shouldProcessNotificationsSequentiallyAndStopOnFailurePerShipment() {
        ClientNotificationGateway gateway = mock(ClientNotificationGateway.class);
        NotificationResultService resultService = mock(NotificationResultService.class);
        NotificationDispatchService dispatchService = new NotificationDispatchService(List.of(gateway), resultService);

        NotificationBatchItem first = buildItem(1L, 10L, LocalDateTime.of(2026, 4, 5, 10, 0));
        NotificationBatchItem second = buildItem(2L, 10L, LocalDateTime.of(2026, 4, 5, 10, 1));
        NotificationBatchItem third = buildItem(3L, 20L, LocalDateTime.of(2026, 4, 5, 10, 2));
        NotificationProcessingResult firstResult = NotificationProcessingResult.success(1L, 200, "{}", "{\"ok\":true}");
        NotificationProcessingResult secondResult = NotificationProcessingResult.retryableError(2L, 500, "{}", "{\"ok\":false}", "HTTP_500", "temporary error");
        NotificationProcessingResult thirdResult = NotificationProcessingResult.success(3L, 200, "{}", "{\"ok\":true}");

        when(gateway.clientId()).thenReturn("SERHAFEN");
        when(gateway.groupByShipment()).thenReturn(true);
        when(gateway.fetchPendingBatch()).thenReturn(List.of(second, third, first));
        when(gateway.process(first)).thenReturn(firstResult);
        when(gateway.process(second)).thenReturn(secondResult);
        when(gateway.process(third)).thenReturn(thirdResult);

        dispatchService.dispatchAll();

        var ordered = inOrder(gateway, resultService);
        ordered.verify(gateway).fetchPendingBatch();
        ordered.verify(gateway).process(first);
        ordered.verify(resultService).registerResult(first, firstResult);
        ordered.verify(gateway).process(second);
        ordered.verify(resultService).registerResult(second, secondResult);
        ordered.verify(gateway).process(third);
        ordered.verify(resultService).registerResult(third, thirdResult);
        verify(gateway).process(third);
    }

    @Test
    void shouldProcessNotificationsOneByOneWhenGatewayDoesNotGroupByShipment() {
        ClientNotificationGateway gateway = mock(ClientNotificationGateway.class);
        NotificationResultService resultService = mock(NotificationResultService.class);
        NotificationDispatchService dispatchService = new NotificationDispatchService(List.of(gateway), resultService);

        NotificationBatchItem first = buildItem(1L, 10L, LocalDateTime.of(2026, 4, 5, 10, 0));
        NotificationBatchItem second = buildItem(2L, 10L, LocalDateTime.of(2026, 4, 5, 10, 1));
        NotificationProcessingResult firstResult = NotificationProcessingResult.retryableError(1L, 500, "{}", "{}", "HTTP_500", "temporary error");
        NotificationProcessingResult secondResult = NotificationProcessingResult.success(2L, 200, "{}", "{\"ok\":true}");

        when(gateway.clientId()).thenReturn("SERHAFEN");
        when(gateway.groupByShipment()).thenReturn(false);
        when(gateway.fetchPendingBatch()).thenReturn(List.of(first, second));
        when(gateway.process(first)).thenReturn(firstResult);
        when(gateway.process(second)).thenReturn(secondResult);

        dispatchService.dispatchAll();

        var ordered = inOrder(gateway, resultService);
        ordered.verify(gateway).fetchPendingBatch();
        ordered.verify(gateway).process(first);
        ordered.verify(resultService).registerResult(first, firstResult);
        ordered.verify(gateway).process(second);
        ordered.verify(resultService).registerResult(second, secondResult);
    }

    private NotificationBatchItem buildItem(Long id, Long shipmentId, LocalDateTime createdAt) {
        return new NotificationBatchItem(
                id,
                "SERHAFEN",
                shipmentId,
                "ORDER-" + id,
                "TRACK-" + id,
                "CARRIER-TRACK-" + id,
                "Carrier",
                "CAR",
                null,
                null,
                null,
                null,
                null,
                "EVENT",
                "SUB_EVENT",
                null,
                "01",
                12345L,
                null,
                0,
                createdAt,
                createdAt,
                createdAt,
                createdAt,
                List.of(),
                List.of()
        );
    }
}
