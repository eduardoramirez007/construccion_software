package com.olva.notification.repository.impl;

import com.olva.notification.config.NotificationProperties;
import com.olva.notification.model.NotificationBatchItem;
import com.olva.notification.repository.NotificationRepository;
import com.olva.notification.repository.mapper.NotificationBatchItemRowMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class OracleNotificationRepository implements NotificationRepository {

    private static final Logger log = LoggerFactory.getLogger(OracleNotificationRepository.class);
    private static final NotificationBatchItemRowMapper BATCH_ITEM_ROW_MAPPER = new NotificationBatchItemRowMapper();

    private static final String SELECT_PENDING_RIPLEY_BATCH_SQL = """
            SELECT *
            FROM (
                SELECT o.ID,
                       :clientId AS CLIENT_ID,
                       o.ID_ENVIO,
                       CAST(NULL AS VARCHAR2(100)) AS ORDER_NUMBER,
                       CAST(NULL AS VARCHAR2(100)) AS TRACKING_NUMBER,
                       CAST(NULL AS VARCHAR2(100)) AS CARRIER_TRACKING_NUMBER,
                       CAST(NULL AS VARCHAR2(100)) AS CARRIER_NAME,
                       CAST(NULL AS VARCHAR2(100)) AS CARRIER_CODE,
                       iic.CUD_NUMBER AS CUD_NUMBER,
                       ea.PIEZA,
                       o.ID_EVENTO,
                       o.ID_ESTADO,
                       o.ID_RPT_ENVIO_RUTA AS RPT_ENVIO_RUTA_ID,
                       h.CODIGO_EVENTO_CLIENTE,
                       h.DESCRIPCION_EVENTO_CLIENTE,
                       o.ID_HOMOLOGO,
                       o.EMISION,
                       o.REMITO,
                       o.GLOSA,
                       NVL(o.LAST_NRO_INTENTO, 0) AS LAST_NRO_INTENTO,
                       o.CREATE_TIME,
                       o.MODIFY_TIME,
                       CAST(NULL AS TIMESTAMP) AS DEAD_LINE_DATE,
                       CAST(NULL AS VARCHAR2(4000)) AS TRACKING_URLS,
                       e.CREATE_TIME AS CREATE_TIME_ORDER
                  FROM CLIENT_NOTIFICATION_OUTBOX o
                  JOIN ENVIO e
                    ON e.ID = o.ID_ENVIO
                  INNER JOIN ENVIO_ARTICULO ea
                    ON ea.ID_ENVIO = e.ID
                  INNER JOIN ITEMS_INTEGRACION_CLIENTE iic
                    ON iic.ID_ENVIO_ARTICULO = ea.ID
                  JOIN CLIENT_EVENT_HOMOLOGATION h
                    ON h.ID = o.ID_HOMOLOGO
                 WHERE h.ID_PERSONA = :personId
                   AND h.ESTADO = '1'
                   AND o.ID_ESTADO = :pendingStateId
                   AND NVL(o.LAST_NRO_INTENTO, 0) < :maxAttempts
                 ORDER BY o.ID_ENVIO, o.CREATE_TIME, ea.PIEZA
            )
            WHERE ROWNUM <= :batchSize
            """;

    private static final String SELECT_PENDING_SERHAFEN_BATCH_SQL = """
            /*
             * TODO SERHAFEN:
             * Reemplazar este SELECT por el SQL oficial para obtener pendientes.
             * Debe retornar exactamente estos alias para reutilizar NotificationBatchItemRowMapper:
             *
             * ID, CLIENT_ID, ID_ENVIO, ORDER_NUMBER, TRACKING_NUMBER, CARRIER_TRACKING_NUMBER,
             * CARRIER_NAME, CARRIER_CODE, CUD_NUMBER, PIEZA, ID_EVENTO, ID_ESTADO,
             * RPT_ENVIO_RUTA_ID, CODIGO_EVENTO_CLIENTE, DESCRIPCION_EVENTO_CLIENTE,
             * ID_HOMOLOGO, EMISION, REMITO, GLOSA, LAST_NRO_INTENTO,
             * CREATE_TIME, MODIFY_TIME, DEAD_LINE_DATE, TRACKING_URLS, CREATE_TIME_ORDER
             *
             * Para Serhafen no se agrupa por CUD_NUMBER; CUD_NUMBER y PIEZA pueden venir NULL.
             * TRACKING_URLS debe devolverse como una cadena separada por comas.
             */
            SELECT *
            FROM (
                SELECT o.ID,
                       :clientId AS CLIENT_ID,
                       o.ID_ENVIO,
                       CAST(NULL AS VARCHAR2(100)) AS ORDER_NUMBER,
                       CAST(NULL AS VARCHAR2(100)) AS TRACKING_NUMBER,
                       CAST(NULL AS VARCHAR2(100)) AS CARRIER_TRACKING_NUMBER,
                       CAST(NULL AS VARCHAR2(100)) AS CARRIER_NAME,
                       CAST(NULL AS VARCHAR2(100)) AS CARRIER_CODE,
                       CAST(NULL AS VARCHAR2(100)) AS CUD_NUMBER,
                       CAST(NULL AS VARCHAR2(100)) AS PIEZA,
                       o.ID_EVENTO,
                       o.ID_ESTADO,
                       o.ID_RPT_ENVIO_RUTA AS RPT_ENVIO_RUTA_ID,
                       h.CODIGO_EVENTO_CLIENTE,
                       h.DESCRIPCION_EVENTO_CLIENTE,
                       o.ID_HOMOLOGO,
                       o.EMISION,
                       o.REMITO,
                       o.GLOSA,
                       NVL(o.LAST_NRO_INTENTO, 0) AS LAST_NRO_INTENTO,
                       o.CREATE_TIME,
                       o.MODIFY_TIME,
                       CAST(NULL AS TIMESTAMP) AS DEAD_LINE_DATE,
                       CAST(NULL AS VARCHAR2(4000)) AS TRACKING_URLS,
                       e.CREATE_TIME AS CREATE_TIME_ORDER
                  FROM CLIENT_NOTIFICATION_OUTBOX o
                  JOIN ENVIO e
                    ON e.ID = o.ID_ENVIO
                  JOIN CLIENT_EVENT_HOMOLOGATION h
                    ON h.ID = o.ID_HOMOLOGO
                 WHERE 1 = 0
                   AND o.ID_ESTADO = :pendingStateId
                   AND NVL(o.LAST_NRO_INTENTO, 0) < :maxAttempts
                 ORDER BY o.CREATE_TIME, o.ID
            )
            WHERE ROWNUM <= :batchSize
            """;

    private static final String UPDATE_LAST_RESULT_SQL = """
            UPDATE CLIENT_NOTIFICATION_OUTBOX
               SET ID_ESTADO = :stateId,
                   LAST_FECHA_PROCESADO = :processedAt,
                   LAST_PAYLOAD = :requestPayload,
                   LAST_CODE_STATUS = :httpStatus,
                   LAST_RESPONSE = :responseBody,
                   LAST_NRO_INTENTO = :attemptCount,
                   MODIFY_TIME = :processedAt
             WHERE ID = :notificationId
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final NotificationProperties notificationProperties;

    public OracleNotificationRepository(NamedParameterJdbcTemplate jdbcTemplate,
                                        NotificationProperties notificationProperties) {
        this.jdbcTemplate = jdbcTemplate;
        this.notificationProperties = notificationProperties;
    }

    @Override
    public List<NotificationBatchItem> claimPendingByPersonId(Long personId,
                                                              String clientId,
                                                              int batchSize,
                                                              int maxAttempts) {
        if (personId == null) {
            log.warn("event=notification_claim_skipped clientId={} reason=missing_person_id", clientId);
            return List.of();
        }

        List<NotificationBatchItem> rawRows = jdbcTemplate.query(
                SELECT_PENDING_RIPLEY_BATCH_SQL,
                new MapSqlParameterSource()
                        .addValue("personId", personId)
                        .addValue("clientId", clientId)
                        .addValue("batchSize", batchSize)
                        .addValue("pendingStateId", notificationProperties.getPendingStateId())
                        .addValue("maxAttempts", maxAttempts),
                BATCH_ITEM_ROW_MAPPER
        );

        List<NotificationBatchItem> pending = aggregateByNotification(rawRows);

        if (pending.isEmpty()) {
            log.info("event=notification_claim_empty clientId={} personId={} batchSize={} maxAttempts={}",
                    clientId,
                    personId,
                    batchSize,
                    maxAttempts);
            return pending;
        }

        log.info("event=notification_claim_success clientId={} personId={} batchSize={} claimedCount={} firstNotificationId={} lastNotificationId={}",
                clientId,
                personId,
                batchSize,
                pending.size(),
                pending.getFirst().id(),
                pending.getLast().id());

        return pending;
    }

    @Override
    public List<NotificationBatchItem> claimPendingSerhafen(String clientId, int batchSize, int maxAttempts) {
        List<NotificationBatchItem> pending = jdbcTemplate.query(
                SELECT_PENDING_SERHAFEN_BATCH_SQL,
                new MapSqlParameterSource()
                        .addValue("clientId", clientId)
                        .addValue("batchSize", batchSize)
                        .addValue("pendingStateId", notificationProperties.getPendingStateId())
                        .addValue("maxAttempts", maxAttempts),
                BATCH_ITEM_ROW_MAPPER
        );

        log.info("event=serhafen_claim_finished clientId={} batchSize={} maxAttempts={} fetchedCount={}",
                clientId,
                batchSize,
                maxAttempts,
                pending.size());

        return pending;
    }

    private List<NotificationBatchItem> aggregateByNotification(List<NotificationBatchItem> rawRows) {
        Map<Long, NotificationAggregation> grouped = new LinkedHashMap<>();

        for (NotificationBatchItem row : rawRows) {
            NotificationAggregation aggregation = grouped.computeIfAbsent(
                    row.id(),
                    ignored -> new NotificationAggregation(row)
            );
            aggregation.addCudNumbers(row.cudNumbers());
        }

        List<NotificationBatchItem> aggregated = new ArrayList<>();
        for (NotificationAggregation aggregation : grouped.values()) {
            aggregated.add(aggregation.toItem());
        }
        return aggregated;
    }

    private static final class NotificationAggregation {
        private final NotificationBatchItem base;
        private final List<String> cudNumbers = new ArrayList<>();

        private NotificationAggregation(NotificationBatchItem base) {
            this.base = base;
            addCudNumbers(base.cudNumbers());
        }

        private void addCudNumbers(List<String> values) {
            if (values == null) {
                return;
            }
            for (String value : values) {
                if (value != null && !value.isBlank() && !cudNumbers.contains(value)) {
                    cudNumbers.add(value);
                }
            }
        }

        private NotificationBatchItem toItem() {
            String firstCud = cudNumbers.isEmpty() ? base.cudNumber() : cudNumbers.getFirst();
            return new NotificationBatchItem(
                    base.id(),
                    base.clientId(),
                    base.shipmentId(),
                    base.orderNumber(),
                    base.trackingNumber(),
                    base.carrierTrackingNumber(),
                    base.carrierName(),
                    base.carrierCode(),
                    firstCud,
                    base.pieceNumber(),
                    base.eventId(),
                    base.stateId(),
                    base.rptEnvioRutaId(),
                    base.eventCode(),
                    base.eventDescription(),
                    base.homologationId(),
                    base.emision(),
                    base.remito(),
                    base.glosa(),
                    base.attemptCount(),
                    base.createdAt(),
                    base.updatedAt(),
                    base.deadlineDate(),
                    base.orderCreatedAt(),
                    List.copyOf(cudNumbers),
                    base.trackingUrls()
            );
        }
    }

    @Override
    public void markAsSent(Long notificationId,
                           int nextAttemptCount,
                           Integer httpStatus,
                           String responseBody,
                           String requestPayload,
                           LocalDateTime processedAt) {
        jdbcTemplate.update(
                UPDATE_LAST_RESULT_SQL,
                commonParams(notificationId, nextAttemptCount, httpStatus, responseBody, requestPayload)
                        .addValue("stateId", notificationProperties.getDeliveredStateId())
                        .addValue("processedAt", Timestamp.valueOf(processedAt))
        );
        log.info("event=notification_outbox_updated notificationId={} status=SENT attemptCount={} httpStatus={}",
                notificationId,
                nextAttemptCount,
                httpStatus);
    }

    @Override
    public void markAsRetry(Long notificationId,
                            int nextAttemptCount,
                            Integer httpStatus,
                            String responseBody,
                            String requestPayload,
                            String errorCode,
                            String errorMessage) {
        LocalDateTime processedAt = LocalDateTime.now();
        jdbcTemplate.update(
                UPDATE_LAST_RESULT_SQL,
                commonParams(notificationId, nextAttemptCount, httpStatus, responseBody, requestPayload)
                        .addValue("stateId", notificationProperties.getPendingStateId())
                        .addValue("processedAt", Timestamp.valueOf(processedAt))
        );
        log.warn("event=notification_outbox_updated notificationId={} status=RETRY attemptCount={} errorCode={} httpStatus={}",
                notificationId,
                nextAttemptCount,
                errorCode,
                httpStatus);
    }

    @Override
    public void markAsFailed(Long notificationId,
                             int nextAttemptCount,
                             Integer httpStatus,
                             String responseBody,
                             String requestPayload,
                             String errorCode,
                             String errorMessage,
                             LocalDateTime processedAt) {
        jdbcTemplate.update(
                UPDATE_LAST_RESULT_SQL,
                commonParams(notificationId, nextAttemptCount, httpStatus, responseBody, requestPayload)
                        .addValue("stateId", notificationProperties.getPendingStateId())
                        .addValue("processedAt", Timestamp.valueOf(processedAt))
        );
        log.error("event=notification_outbox_updated notificationId={} status=FAILED attemptCount={} errorCode={} httpStatus={}",
                notificationId,
                nextAttemptCount,
                errorCode,
                httpStatus);
    }

    @Override
    public void markAsDiscarded(Long notificationId,
                                int nextAttemptCount,
                                String requestPayload,
                                String responseBody,
                                String errorCode,
                                String errorMessage,
                                LocalDateTime processedAt) {
        jdbcTemplate.update(
                UPDATE_LAST_RESULT_SQL,
                commonParams(notificationId, nextAttemptCount, -1, responseBody, requestPayload)
                        .addValue("stateId", notificationProperties.getPendingStateId())
                        .addValue("processedAt", Timestamp.valueOf(processedAt))
        );
        log.warn("event=notification_outbox_updated notificationId={} status=DISCARDED attemptCount={} errorCode={}",
                notificationId,
                nextAttemptCount,
                errorCode);
    }

    private MapSqlParameterSource commonParams(Long notificationId,
                                               int nextAttemptCount,
                                               Integer httpStatus,
                                               String responseBody,
                                               String requestPayload) {
        return new MapSqlParameterSource()
                .addValue("notificationId", notificationId)
                .addValue("attemptCount", nextAttemptCount)
                .addValue("httpStatus", httpStatus != null ? httpStatus : -1)
                .addValue("responseBody", responseBody)
                .addValue("requestPayload", requestPayload);
    }
}
