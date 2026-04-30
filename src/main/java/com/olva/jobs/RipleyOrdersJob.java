package com.olva.jobs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.olva.client.RipleyOrdersClient;
import com.olva.dto.RipleyOrderDTO;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RipleyOrdersJob extends QuartzJobBean {

    private static final Logger log = LoggerFactory.getLogger(RipleyOrdersJob.class);

    private final RipleyOrdersClient ripleyOrdersClient;

    public RipleyOrdersJob(RipleyOrdersClient ripleyOrdersClient) {
        this.ripleyOrdersClient = ripleyOrdersClient;
    }

    @Override
    protected void executeInternal(JobExecutionContext context) {
        //List<RipleyOrderDTO> response = ripleyOrdersClient.getUnprocessedOrders();
        //log.info("Respuesta ordenes no consultadas: {}", toJson(response));
        log.info("Order Job log.. no ejecuta");
    }

    /*private String toJson(List<RipleyOrderDTO> response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar la respuesta de ordenes Ripley", e);
        }
    }*/
}
