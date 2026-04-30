package com.olva.notification.repository.impl;

import com.olva.notification.model.NotificationAttemptRecord;
import com.olva.notification.repository.NotificationAttemptRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;

@Repository
public class OracleNotificationAttemptRepository implements NotificationAttemptRepository {

    private static final Logger log = LoggerFactory.getLogger(OracleNotificationAttemptRepository.class);

    private static final String INSERT_ATTEMPT_SQL = """
            INSERT INTO CLIENT_NOTIFICATION_LOG (
                ID,
                ID_OUTBOX,
                ID_ENVIO,
                ID_EVENTO,
                EMISION,
                REMITO,
                NRO_INTENTO,
                ESTADO,
                REQUEST_BODY,
                RESPONSE_CODE,
                RESPONSE_BODY,
                MENSAJE,
                CREATE_DATE
            ) VALUES (
                (SELECT NVL(MAX(ID), 0) + 1 FROM CLIENT_NOTIFICATION_LOG),
                :outboxId,
                :shipmentId,
                :eventId,
                :emision,
                :remito,
                :attemptNumber,
                :status,
                :requestPayload,
                :responseCode,
                :responseBody,
                :message,
                :attemptedAt
            )
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public OracleNotificationAttemptRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void save(NotificationAttemptRecord attemptRecord) {
        jdbcTemplate.update(
                INSERT_ATTEMPT_SQL,
                new MapSqlParameterSource()
                        .addValue("outboxId", attemptRecord.outboxId())
                        .addValue("shipmentId", attemptRecord.shipmentId())
                        .addValue("eventId", attemptRecord.eventId())
                        .addValue("emision", attemptRecord.emision())
                        .addValue("remito", attemptRecord.remito())
                        .addValue("attemptNumber", attemptRecord.attemptNumber())
                        .addValue("status", attemptRecord.status())
                        .addValue("requestPayload", attemptRecord.requestPayload())
                        .addValue("responseCode", attemptRecord.responseCode())
                        .addValue("responseBody", attemptRecord.responseBody())
                        .addValue("message", truncate(attemptRecord.message(), 1000))
                        .addValue("attemptedAt", Timestamp.valueOf(attemptRecord.attemptedAt()))
        );
        log.info("event=notification_attempt_saved notificationId={} shipmentId={} attemptNumber={} status={} responseCode={}",
                attemptRecord.outboxId(),
                attemptRecord.shipmentId(),
                attemptRecord.attemptNumber(),
                attemptRecord.status(),
                attemptRecord.responseCode());
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
