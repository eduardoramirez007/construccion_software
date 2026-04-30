package com.olva.notification.gateway.serhafen;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SerhafenDispatchTrackingRequest(
        @JsonProperty("identifier")
        Long identifier,
        @JsonProperty("order_number")
        String orderNumber,
        @JsonProperty("tracking_number")
        String trackingNumber,
        @JsonProperty("carrier_tracking_number")
        String carrierTrackingNumber,
        @JsonProperty("carrier_name")
        String carrierName,
        @JsonProperty("carrier_code")
        String carrierCode,
        @JsonProperty("dead_line_date")
        String deadLineDate,
        @JsonProperty("status_id")
        String statusId,
        @JsonProperty("status_name")
        String statusName,
        @JsonProperty("status_information")
        String statusInformation,
        @JsonProperty("status_date")
        String statusDate,
        @JsonProperty("tracking_url")
        List<String> trackingUrl
) {
}
