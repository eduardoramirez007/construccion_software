package com.olva.notification.gateway.serhafen;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.olva.notification.config.NotificationProperties;
import com.olva.notification.config.SerhafenNotificationProperties;
import com.olva.notification.gateway.ClientNotificationGateway;
import com.olva.notification.model.NotificationBatchItem;
import com.olva.notification.model.NotificationProcessingResult;
import com.olva.notification.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class SerhafenNotificationGateway implements ClientNotificationGateway {

    private static final Logger log = LoggerFactory.getLogger(SerhafenNotificationGateway.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String API_KEY_HEADER = "x-api-key";
    private static final String EXECUTOR_NAME_HEADER = "executor-name";

    private final RestTemplate restTemplate;
    private final NotificationRepository notificationRepository;
    private final NotificationProperties notificationProperties;
    private final SerhafenNotificationProperties serhafenNotificationProperties;
    private final ObjectMapper objectMapper;

    public SerhafenNotificationGateway(RestTemplate restTemplate,
                                       NotificationRepository notificationRepository,
                                       NotificationProperties notificationProperties,
                                       SerhafenNotificationProperties serhafenNotificationProperties) {
        this.restTemplate = restTemplate;
        this.notificationRepository = notificationRepository;
        this.notificationProperties = notificationProperties;
        this.serhafenNotificationProperties = serhafenNotificationProperties;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String clientId() {
        return "SERHAFEN";
    }

    @Override
    public boolean groupByShipment() {
        return false;
    }

    @Override
    public List<NotificationBatchItem> fetchPendingBatch() {
        List<NotificationBatchItem> batch = notificationRepository.claimPendingSerhafen(
                clientId(),
                notificationProperties.getBatchSize(),
                notificationProperties.getMaxAttempts()
        );

        log.info("event=serhafen_batch_fetched clientId={} batchSize={} fetchedCount={}",
                clientId(),
                notificationProperties.getBatchSize(),
                batch.size());
        log.debug("event=serhafen_batch_debug clientId={} notificationIds={}",
                clientId(),
                batch.stream().map(NotificationBatchItem::id).toList());

        return batch;
    }

    @Override
    public NotificationProcessingResult process(NotificationBatchItem notification) {
        String requestPayload = null;

        try {
            validateNotificationConfiguration();
            validateNotification(notification);

            requestPayload = buildRequestPayload(notification);
            HttpHeaders headers = buildHeaders();
            HttpEntity<String> request = new HttpEntity<>(requestPayload, headers);

            log.info("event=serhafen_notification_prepare clientId={} notificationId={} shipmentId={} trackingNumber={} carrierTrackingNumber={} statusId={}",
                    clientId(),
                    notification.id(),
                    notification.shipmentId(),
                    notification.trackingNumber(),
                    notification.carrierTrackingNumber(),
                    notification.eventCode());
            log.debug("event=serhafen_notification_payload notificationId={} url={} payload={}",
                    notification.id(),
                    serhafenNotificationProperties.getTrackingUrl(),
                    requestPayload);

            ResponseEntity<String> response = restTemplate.exchange(
                    serhafenNotificationProperties.getTrackingUrl(),
                    HttpMethod.POST,
                    request,
                    String.class
            );

            log.info("event=serhafen_notification_response notificationId={} httpStatus={} responseBody={}",
                    notification.id(),
                    response.getStatusCode().value(),
                    response.getBody());

            return buildResult(notification, requestPayload, response);
        } catch (IllegalStateException e) {
            log.error("event=serhafen_notification_config_error notificationId={} errorMessage={}",
                    notification.id(),
                    e.getMessage());
            return NotificationProcessingResult.retryableError(
                    notification.id(),
                    null,
                    requestPayload,
                    null,
                    "SERHAFEN_CONFIG_ERROR",
                    e.getMessage()
            );
        } catch (IllegalArgumentException e) {
            log.warn("event=serhafen_notification_invalid notificationId={} errorMessage={}",
                    notification.id(),
                    e.getMessage());
            return NotificationProcessingResult.nonRetryableError(
                    notification.id(),
                    null,
                    requestPayload,
                    null,
                    "DISCARDED",
                    e.getMessage()
            );
        } catch (RestClientResponseException e) {
            log.error("event=serhafen_notification_http_error notificationId={} httpStatus={} responseBody={}",
                    notification.id(),
                    e.getStatusCode().value(),
                    e.getResponseBodyAsString());
            return NotificationProcessingResult.retryableError(
                    notification.id(),
                    e.getStatusCode().value(),
                    requestPayload,
                    e.getResponseBodyAsString(),
                    "HTTP_" + e.getStatusCode().value(),
                    "Serhafen respondio con error HTTP"
            );
        } catch (Exception e) {
            log.error("event=serhafen_notification_unexpected_error notificationId={} message={}",
                    notification.id(),
                    e.getMessage(),
                    e);
            return NotificationProcessingResult.retryableError(
                    notification.id(),
                    null,
                    requestPayload,
                    null,
                    "SERHAFEN_GATEWAY_ERROR",
                    e.getMessage()
            );
        }
    }

    private String buildRequestPayload(NotificationBatchItem notification) {
        try {
            SerhafenDispatchTrackingRequest request = new SerhafenDispatchTrackingRequest(
                    notification.shipmentId(),
                    notification.orderNumber(),
                    notification.trackingNumber(),
                    notification.carrierTrackingNumber(),
                    notification.carrierName(),
                    notification.carrierCode(),
                    formatDate(notification.deadlineDate()),
                    notification.eventCode(),
                    notification.eventDescription(),
                    notification.glosa(),
                    formatDate(resolveStatusDate(notification)),
                    sanitizeTrackingUrls(notification.trackingUrls())
            );
            return objectMapper.writeValueAsString(request);
        } catch (Exception e) {
            throw new IllegalArgumentException("No se pudo construir el payload de trazabilidad Serhafen", e);
        }
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(API_KEY_HEADER, serhafenNotificationProperties.getApiKey());
        headers.set(EXECUTOR_NAME_HEADER, serhafenNotificationProperties.getExecutorName());
        return headers;
    }

    private void validateNotificationConfiguration() {
        requireConfiguration(serhafenNotificationProperties.getTrackingUrl(), "integration.serhafen.notification.tracking-url");
        requireConfiguration(serhafenNotificationProperties.getApiKey(), "integration.serhafen.notification.api-key");
        requireConfiguration(serhafenNotificationProperties.getExecutorName(), "integration.serhafen.notification.executor-name");
    }

    private void validateNotification(NotificationBatchItem notification) {
        if (notification.shipmentId() == null) {
            throw new IllegalArgumentException("La notificacion Serhafen no tiene ID_ENVIO");
        }
        requireValue(notification.trackingNumber(), "SERHAFEN.TRACKING_NUMBER");
        requireValue(notification.carrierTrackingNumber(), "SERHAFEN.CARRIER_TRACKING_NUMBER");
        requireValue(notification.eventCode(), "CLIENT_EVENT_HOMOLOGATION.CODIGO_EVENTO_CLIENTE");
        requireValue(notification.eventDescription(), "CLIENT_EVENT_HOMOLOGATION.DESCRIPCION_EVENTO_CLIENTE");
        if (resolveStatusDate(notification) == null) {
            throw new IllegalArgumentException("La notificacion Serhafen no tiene fecha para STATUS_DATE");
        }
    }

    private NotificationProcessingResult buildResult(NotificationBatchItem notification,
                                                     String requestPayload,
                                                     ResponseEntity<String> response) {
        if (response.getStatusCode().is2xxSuccessful()) {
            return NotificationProcessingResult.success(
                    notification.id(),
                    response.getStatusCode().value(),
                    requestPayload,
                    response.getBody()
            );
        }

        return NotificationProcessingResult.retryableError(
                notification.id(),
                response.getStatusCode().value(),
                requestPayload,
                response.getBody(),
                "HTTP_" + response.getStatusCode().value(),
                "Serhafen respondio con estado HTTP no exitoso"
        );
    }

    private LocalDateTime resolveStatusDate(NotificationBatchItem notification) {
        if (notification.updatedAt() != null) {
            return notification.updatedAt();
        }
        return notification.createdAt();
    }

    private List<String> sanitizeTrackingUrls(List<String> trackingUrls) {
        if (trackingUrls == null || trackingUrls.isEmpty()) {
            return null;
        }
        List<String> sanitized = trackingUrls.stream()
                .filter(this::hasText)
                .map(String::trim)
                .toList();
        return sanitized.isEmpty() ? null : sanitized;
    }

    private String formatDate(LocalDateTime value) {
        return value != null ? value.format(DATE_TIME_FORMATTER) : null;
    }

    private void requireValue(String value, String fieldName) {
        if (!hasText(value)) {
            throw new IllegalArgumentException("Falta informar el campo requerido: " + fieldName);
        }
    }

    private void requireConfiguration(String value, String fieldName) {
        if (!hasText(value)) {
            throw new IllegalStateException("Falta configurar el campo requerido: " + fieldName);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
