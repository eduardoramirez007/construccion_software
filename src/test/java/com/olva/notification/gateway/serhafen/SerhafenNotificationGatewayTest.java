package com.olva.notification.gateway.serhafen;

import com.olva.notification.config.NotificationProperties;
import com.olva.notification.config.SerhafenNotificationProperties;
import com.olva.notification.model.NotificationBatchItem;
import com.olva.notification.model.NotificationProcessingResult;
import com.olva.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SerhafenNotificationGatewayTest {

    private RestTemplate restTemplate;
    private NotificationRepository notificationRepository;
    private NotificationProperties notificationProperties;
    private SerhafenNotificationProperties serhafenNotificationProperties;
    private SerhafenNotificationGateway gateway;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        notificationRepository = mock(NotificationRepository.class);
        notificationProperties = new NotificationProperties();
        notificationProperties.setBatchSize(50);
        notificationProperties.setMaxAttempts(3);
        serhafenNotificationProperties = new SerhafenNotificationProperties();
        serhafenNotificationProperties.setTrackingUrl("https://serhafen.example.test/package-status");
        serhafenNotificationProperties.setApiKey("secret");
        serhafenNotificationProperties.setExecutorName("DNT");
        gateway = new SerhafenNotificationGateway(
                restTemplate,
                notificationRepository,
                notificationProperties,
                serhafenNotificationProperties
        );
    }

    @Test
    void shouldFetchPendingBatchFromSerhafenRepository() {
        NotificationBatchItem item = buildItem();
        when(notificationRepository.claimPendingSerhafen("SERHAFEN", 50, 3)).thenReturn(List.of(item));

        List<NotificationBatchItem> batch = gateway.fetchPendingBatch();

        assertThat(batch).containsExactly(item);
        assertThat(gateway.groupByShipment()).isFalse();
        verify(notificationRepository).claimPendingSerhafen("SERHAFEN", 50, 3);
    }

    @Test
    void shouldSendPayloadAndReturnSuccessOnHttp2xx() {
        when(restTemplate.exchange(
                eq("https://serhafen.example.test/package-status"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(ResponseEntity.ok("\"Package status updated successfully\""));

        NotificationProcessingResult result = gateway.process(buildItem());

        assertThat(result.successful()).isTrue();
        assertThat(result.httpStatus()).isEqualTo(200);
        assertThat(result.requestPayload()).contains("\"identifier\":100");
        assertThat(result.requestPayload()).contains("\"tracking_number\":\"SH-PE-123456789\"");
        assertThat(result.requestPayload()).contains("\"carrier_tracking_number\":\"DNT-PE-789456123\"");
        assertThat(result.requestPayload()).contains("\"status_id\":\"DNT-100\"");
        assertThat(result.requestPayload()).contains("\"tracking_url\":[\"https://tracking.dinet.pe/evidence/image1.jpg\",\"https://tracking.dinet.pe/evidence/image2.jpg\"]");

        ArgumentCaptor<HttpEntity<String>> requestCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(
                eq("https://serhafen.example.test/package-status"),
                eq(HttpMethod.POST),
                requestCaptor.capture(),
                eq(String.class)
        );
        assertThat(requestCaptor.getValue().getHeaders().getFirst("x-api-key")).isEqualTo("secret");
        assertThat(requestCaptor.getValue().getHeaders().getFirst("executor-name")).isEqualTo("DNT");
        assertThat(requestCaptor.getValue().getBody()).contains("\"carrier_name\":\"Dinet\"");
        assertThat(requestCaptor.getValue().getBody()).contains("\"carrier_code\":\"DNT\"");
    }

    @Test
    void shouldReturnRetryableErrorOnHttpException() {
        when(restTemplate.exchange(
                eq("https://serhafen.example.test/package-status"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class)
        )).thenThrow(new HttpServerErrorException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "server error",
                "serhafen down".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8
        ));

        NotificationProcessingResult result = gateway.process(buildItem());

        assertThat(result.successful()).isFalse();
        assertThat(result.retryable()).isTrue();
        assertThat(result.httpStatus()).isEqualTo(500);
        assertThat(result.errorCode()).isEqualTo("HTTP_500");
        assertThat(result.responseBody()).isEqualTo("serhafen down");
    }

    @Test
    void shouldDiscardWhenRequiredDataIsMissing() {
        NotificationProcessingResult result = gateway.process(buildItemWithoutCarrierTrackingNumber());

        assertThat(result.successful()).isFalse();
        assertThat(result.retryable()).isFalse();
        assertThat(result.errorCode()).isEqualTo("DISCARDED");
        assertThat(result.errorMessage()).contains("CARRIER_TRACKING_NUMBER");
        verify(restTemplate, never()).exchange(any(String.class), any(HttpMethod.class), any(HttpEntity.class), eq(String.class));
    }

    @Test
    void shouldRetryWhenExecutorNameIsNotConfigured() {
        serhafenNotificationProperties.setExecutorName(null);

        NotificationProcessingResult result = gateway.process(buildItem());

        assertThat(result.successful()).isFalse();
        assertThat(result.retryable()).isTrue();
        assertThat(result.errorCode()).isEqualTo("SERHAFEN_CONFIG_ERROR");
        assertThat(result.errorMessage()).contains("executor-name");
        verify(restTemplate, never()).exchange(any(String.class), any(HttpMethod.class), any(HttpEntity.class), eq(String.class));
    }

    private NotificationBatchItem buildItem() {
        return new NotificationBatchItem(
                1L,
                "SERHAFEN",
                100L,
                "ORDER-123",
                "SH-PE-123456789",
                "DNT-PE-789456123",
                "Dinet",
                "DNT",
                null,
                null,
                200L,
                9140L,
                300L,
                "DNT-100",
                "En ruta de entrega",
                400L,
                "01",
                12345L,
                "Paquete en camion de reparto",
                0,
                LocalDateTime.of(2026, 4, 28, 10, 0),
                LocalDateTime.of(2026, 4, 28, 10, 5),
                LocalDateTime.of(2026, 4, 29, 18, 0),
                LocalDateTime.of(2026, 4, 28, 9, 0),
                List.of(),
                List.of(
                        "https://tracking.dinet.pe/evidence/image1.jpg",
                        "https://tracking.dinet.pe/evidence/image2.jpg"
                )
        );
    }

    private NotificationBatchItem buildItemWithoutCarrierTrackingNumber() {
        NotificationBatchItem item = buildItem();
        return new NotificationBatchItem(
                item.id(),
                item.clientId(),
                item.shipmentId(),
                item.orderNumber(),
                item.trackingNumber(),
                null,
                item.carrierName(),
                item.carrierCode(),
                item.cudNumber(),
                item.pieceNumber(),
                item.eventId(),
                item.stateId(),
                item.rptEnvioRutaId(),
                item.eventCode(),
                item.eventDescription(),
                item.homologationId(),
                item.emision(),
                item.remito(),
                item.glosa(),
                item.attemptCount(),
                item.createdAt(),
                item.updatedAt(),
                item.deadlineDate(),
                item.orderCreatedAt(),
                item.cudNumbers(),
                item.trackingUrls()
        );
    }
}
