package com.olva.notification.gateway.ripley;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.olva.notification.model.NotificationAttemptDetail;
import com.olva.notification.config.NotificationProperties;
import com.olva.notification.config.RipleyNotificationProperties;
import com.olva.notification.gateway.ClientNotificationGateway;
import com.olva.notification.model.NotificationBatchItem;
import com.olva.notification.model.NotificationProcessingResult;
import com.olva.notification.repository.NotificationRepository;
import com.olva.service.RipleyAuthService;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class RipleyNotificationGateway implements ClientNotificationGateway {

    private static final Logger log = LoggerFactory.getLogger(RipleyNotificationGateway.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final RestTemplate restTemplate;
    private final RipleyAuthService ripleyAuthService;
    private final NotificationRepository notificationRepository;
    private final NotificationProperties notificationProperties;
    private final RipleyNotificationProperties ripleyNotificationProperties;
    private final ObjectMapper objectMapper;

    public RipleyNotificationGateway(RestTemplate restTemplate,
                                     RipleyAuthService ripleyAuthService,
                                     NotificationRepository notificationRepository,
                                     NotificationProperties notificationProperties,
                                     RipleyNotificationProperties ripleyNotificationProperties) {
        this.restTemplate = restTemplate;
        this.ripleyAuthService = ripleyAuthService;
        this.notificationRepository = notificationRepository;
        this.notificationProperties = notificationProperties;
        this.ripleyNotificationProperties = ripleyNotificationProperties;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String clientId() {
        return "RIPLEY";
    }

    @Override
    public List<NotificationBatchItem> fetchPendingBatch() {
        List<NotificationBatchItem> batch = notificationRepository.claimPendingByPersonId(
                ripleyNotificationProperties.getPersonId(),
                clientId(),
                notificationProperties.getBatchSize(),
                notificationProperties.getMaxAttempts()
        );
        log.info("event=ripley_batch_fetched clientId={} batchSize={} fetchedCount={}",
                clientId(),
                notificationProperties.getBatchSize(),
                batch.size());
        return batch;
    }

    @Override
    public NotificationProcessingResult process(NotificationBatchItem notification) {
        try {
            validateNotificationConfiguration();
            validateNotification(notification);

            String accessToken = ripleyAuthService.getValidToken();
            List<NotificationAttemptDetail> attemptDetails = new ArrayList<>();
            NotificationProcessingResult headerResult = null;

            log.info("event=ripley_notification_prepare clientId={} notificationId={} shipmentId={} eventCode={} eventDescription={} cudCount={}",
                    clientId(),
                    notification.id(),
                    notification.shipmentId(),
                    notification.eventCode(),
                    notification.eventDescription(),
                    notification.cudNumbers().size());

            for (String cudNumber : notification.cudNumbers()) {
                NotificationProcessingResult perCudResult = sendByCud(notification, accessToken, cudNumber);
                attemptDetails.add(toAttemptDetail(cudNumber, perCudResult));

                if (headerResult == null) {
                    headerResult = perCudResult;
                }
            }

            if (headerResult == null) {
                return NotificationProcessingResult.nonRetryableError(
                        notification.id(),
                        null,
                        null,
                        null,
                        "DISCARDED",
                        "La notificacion no tiene CUD_NUMBERs para enviar"
                );
            }

            return copyHeaderResultWithAttemptDetails(headerResult, attemptDetails);
        } catch (IllegalArgumentException e) {
            return NotificationProcessingResult.nonRetryableError(
                    notification.id(),
                    null,
                    null,
                    null,
                    "DISCARDED",
                    e.getMessage()
            );
        } catch (RestClientResponseException e) {
            log.error("event=ripley_notification_http_error notificationId={} httpStatus={} responseBody={}",
                    notification.id(),
                    e.getStatusCode().value(),
                    e.getResponseBodyAsString());
            return NotificationProcessingResult.retryableError(
                    notification.id(),
                    e.getStatusCode().value(),
                    null,
                    e.getResponseBodyAsString(),
                    "HTTP_" + e.getStatusCode().value(),
                    "Ripley dispatchTracking respondio con error HTTP"
            );
        } catch (Exception e) {
            log.error("event=ripley_notification_unexpected_error notificationId={} message={}",
                    notification.id(),
                    e.getMessage(),
                    e);
            return NotificationProcessingResult.retryableError(
                    notification.id(),
                    null,
                    null,
                    null,
                    "RIPLEY_GATEWAY_ERROR",
                    e.getMessage()
            );
        }
    }

    private NotificationProcessingResult sendByCud(NotificationBatchItem notification,
                                                   String accessToken,
                                                   String cudNumber) {
        String requestPayload = buildRequestPayload(notification, cudNumber);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        HttpEntity<String> request = new HttpEntity<>(requestPayload, headers);

        log.info("event=ripley_notification_request_details notificationId={} cudNumber={} url={} authorization={} contentType={} payload={}",
                notification.id(),
                cudNumber,
                ripleyNotificationProperties.getTrackingUrl(),
                "Bearer " + accessToken,
                MediaType.APPLICATION_JSON_VALUE,
                requestPayload);
        log.info("event=ripley_notification_request_curl notificationId={} cudNumber={} curl={}",
                notification.id(),
                cudNumber,
                buildCurlCommand(accessToken, requestPayload));

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    ripleyNotificationProperties.getTrackingUrl(),
                    HttpMethod.POST,
                    request,
                    String.class
            );

            log.info("event=ripley_notification_response notificationId={} cudNumber={} httpStatus={} responseBody={}",
                    notification.id(),
                    cudNumber,
                    response.getStatusCode().value(),
                    response.getBody());

            return buildResult(notification, requestPayload, response);
        } catch (RestClientResponseException e) {
            log.error("event=ripley_notification_http_error notificationId={} cudNumber={} httpStatus={} responseBody={}",
                    notification.id(),
                    cudNumber,
                    e.getStatusCode().value(),
                    e.getResponseBodyAsString());
            return NotificationProcessingResult.retryableError(
                    notification.id(),
                    e.getStatusCode().value(),
                    requestPayload,
                    e.getResponseBodyAsString(),
                    "HTTP_" + e.getStatusCode().value(),
                    "Ripley dispatchTracking respondio con error HTTP"
            );
        } catch (Exception e) {
            log.error("event=ripley_notification_unexpected_error notificationId={} cudNumber={} message={}",
                    notification.id(),
                    cudNumber,
                    e.getMessage(),
                    e);
            return NotificationProcessingResult.retryableError(
                    notification.id(),
                    null,
                    requestPayload,
                    null,
                    "RIPLEY_GATEWAY_ERROR",
                    e.getMessage()
            );
        }
    }

    private String buildRequestPayload(NotificationBatchItem notification, String cudNumber) {
        try {
            RipleyDispatchTrackingRequest request = new RipleyDispatchTrackingRequest(
                    ripleyNotificationProperties.getUsername(),
                    ripleyNotificationProperties.getPassword(),
                    cudNumber,
                    notification.eventCode(),
                    formatDate(notification.orderCreatedAt()),
                    formatDate(resolveUpdatedAt(notification))
            );
            return objectMapper.writeValueAsString(request);
        } catch (Exception e) {
            throw new IllegalArgumentException("No se pudo construir el payload de trazabilidad Ripley", e);
        }
    }

    private void validateNotificationConfiguration() {
        require(ripleyNotificationProperties.getTrackingUrl(), "integration.ripley.notification.tracking-url");
        require(ripleyNotificationProperties.getUsername(), "integration.ripley.notification.username");
        require(ripleyNotificationProperties.getPassword(), "integration.ripley.notification.password");
    }

    private void validateNotification(NotificationBatchItem notification) {
        if (notification.cudNumbers() == null || notification.cudNumbers().isEmpty()) {
            throw new IllegalArgumentException("La notificacion no tiene ITEMS_INTEGRACION_CLIENTE.CUD_NUMBER");
        }
        if (notification.shipmentId() == null) {
            throw new IllegalArgumentException("La notificacion no tiene ID_ENVIO");
        }
        require(notification.eventCode(), "CLIENT_EVENT_HOMOLOGATION.CODIGO_EVENTO_CLIENTE");
        if (notification.orderCreatedAt() == null) {
            throw new IllegalArgumentException("La notificacion no tiene ENVIO.CREATE_TIME para fecha_creacion");
        }
    }

    private void require(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Falta configurar el campo requerido: " + fieldName);
        }
    }

    private LocalDateTime resolveUpdatedAt(NotificationBatchItem notification) {
        return notification.updatedAt() != null ? notification.updatedAt() : notification.createdAt();
    }

    private String formatDate(LocalDateTime value) {
        return value.format(DATE_TIME_FORMATTER);
    }

    private String buildCurlCommand(String accessToken, String requestPayload) {
        return "curl --request POST "
                + "--url '" + ripleyNotificationProperties.getTrackingUrl() + "' "
                + "--header 'Content-Type: application/json' "
                + "--header 'Authorization: Bearer " + escapeSingleQuotes(accessToken) + "' "
                + "--data '" + escapeSingleQuotes(requestPayload) + "'";
    }

    private String escapeSingleQuotes(String value) {
        return value == null ? "" : value.replace("'", "'\"'\"'");
    }

    private NotificationProcessingResult buildResult(NotificationBatchItem notification,
                                                     String requestPayload,
                                                     ResponseEntity<String> response) {
        String responseBody = response.getBody();
        String normalizedBody = normalizeResponseBody(responseBody);

        if ("OK".equals(normalizedBody)) {
            return NotificationProcessingResult.success(
                    notification.id(),
                    response.getStatusCode().value(),
                    requestPayload,
                    responseBody
            );
        }

        if ("ERROR".equals(normalizedBody)) {
            log.warn("event=ripley_notification_response_error_body clientId={} notificationId={} shipmentId={} httpStatus={} responseBody={}",
                    clientId(),
                    notification.id(),
                    notification.shipmentId(),
                    response.getStatusCode().value(),
                    responseBody);

            return NotificationProcessingResult.success(
                    notification.id(),
                    response.getStatusCode().value(),
                    requestPayload,
                    responseBody
            );
        }

        return NotificationProcessingResult.retryableError(
                notification.id(),
                response.getStatusCode().value(),
                requestPayload,
                responseBody,
                "UNEXPECTED_BODY",
                "Respuesta no reconocida del servicio dispatchTracking"
        );
    }

    private String normalizeResponseBody(String responseBody) {
        if (responseBody == null) {
            return "";
        }

        String trimmed = responseBody.trim();
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length() >= 2) {
            trimmed = trimmed.substring(1, trimmed.length() - 1);
        }

        return trimmed.trim().toUpperCase(Locale.ROOT);
    }

    private NotificationAttemptDetail toAttemptDetail(String cudNumber, NotificationProcessingResult result) {
        String status;
        if (result.successful()) {
            status = "SUCCESS";
        } else if (result.retryable()) {
            status = "RETRY";
        } else if ("DISCARDED".equalsIgnoreCase(result.errorCode())) {
            status = "DISCARDED";
        } else {
            status = "FAILED";
        }

        return new NotificationAttemptDetail(
                cudNumber,
                status,
                result.requestPayload(),
                result.httpStatus() != null ? String.valueOf(result.httpStatus()) : result.errorCode(),
                result.responseBody(),
                result.successful() ? "Notificacion enviada correctamente" : result.errorMessage()
        );
    }

    private NotificationProcessingResult copyHeaderResultWithAttemptDetails(NotificationProcessingResult headerResult,
                                                                            List<NotificationAttemptDetail> attemptDetails) {
        if (headerResult.successful()) {
            return NotificationProcessingResult.success(
                    headerResult.notificationId(),
                    headerResult.httpStatus(),
                    headerResult.requestPayload(),
                    headerResult.responseBody(),
                    attemptDetails
            );
        }

        if (headerResult.retryable()) {
            return NotificationProcessingResult.retryableError(
                    headerResult.notificationId(),
                    headerResult.httpStatus(),
                    headerResult.requestPayload(),
                    headerResult.responseBody(),
                    headerResult.errorCode(),
                    headerResult.errorMessage(),
                    attemptDetails
            );
        }

        return new NotificationProcessingResult(
                headerResult.notificationId(),
                false,
                false,
                headerResult.httpStatus(),
                headerResult.requestPayload(),
                headerResult.responseBody(),
                headerResult.errorCode(),
                headerResult.errorMessage(),
                attemptDetails
        );
    }
}
