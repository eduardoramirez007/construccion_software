package com.olva.notification.repository.mapper;

import com.olva.notification.model.NotificationBatchItem;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class NotificationBatchItemRowMapper implements RowMapper<NotificationBatchItem> {

    @Override
    public NotificationBatchItem mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new NotificationBatchItem(
                rs.getLong("ID"),
                rs.getString("CLIENT_ID"),
                rs.getLong("ID_ENVIO"),
                rs.getString("ORDER_NUMBER"),
                rs.getString("TRACKING_NUMBER"),
                rs.getString("CARRIER_TRACKING_NUMBER"),
                rs.getString("CARRIER_NAME"),
                rs.getString("CARRIER_CODE"),
                rs.getString("CUD_NUMBER"),
                rs.getString("PIEZA"),
                rs.getLong("ID_EVENTO"),
                getNullableLong(rs, "ID_ESTADO"),
                getNullableLong(rs, "RPT_ENVIO_RUTA_ID"),
                rs.getString("CODIGO_EVENTO_CLIENTE"),
                rs.getString("DESCRIPCION_EVENTO_CLIENTE"),
                getNullableLong(rs, "ID_HOMOLOGO"),
                rs.getString("EMISION"),
                getNullableLong(rs, "REMITO"),
                rs.getString("GLOSA"),
                rs.getInt("LAST_NRO_INTENTO"),
                rs.getTimestamp("CREATE_TIME").toLocalDateTime(),
                rs.getTimestamp("MODIFY_TIME") != null ? rs.getTimestamp("MODIFY_TIME").toLocalDateTime() : null,
                rs.getTimestamp("DEAD_LINE_DATE") != null ? rs.getTimestamp("DEAD_LINE_DATE").toLocalDateTime() : null,
                rs.getTimestamp("CREATE_TIME_ORDER") != null ? rs.getTimestamp("CREATE_TIME_ORDER").toLocalDateTime() : null,
                buildCudNumbers(rs.getString("CUD_NUMBER")),
                buildTrackingUrls(rs.getString("TRACKING_URLS"))
        );
    }

    private List<String> buildCudNumbers(String cudNumber) {
        if (cudNumber == null || cudNumber.isBlank()) {
            return List.of();
        }
        return List.of(cudNumber);
    }

    private List<String> buildTrackingUrls(String trackingUrls) {
        if (trackingUrls == null || trackingUrls.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(trackingUrls.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
    }

    private Long getNullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }
}
