package com.olva.client;

import com.olva.dto.RipleyOrderDTO;
import com.olva.service.RipleyAuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Component
public class RipleyOrdersClient {

    private final RestTemplate restTemplate;
    private final RipleyAuthService authService;

    @Value("${integration.ripley.orders.url}")
    private String baseUrl;

    @Value("${integration.ripley.opl-code}")
    private String oplCode;

    public RipleyOrdersClient(RestTemplate restTemplate,
                              RipleyAuthService authService) {
        this.restTemplate = restTemplate;
        this.authService = authService;
    }

    public List<RipleyOrderDTO> getUnprocessedOrders() {

        String url = baseUrl + "/" + oplCode;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authService.getValidToken());

        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<List<RipleyOrderDTO>> response =
                restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        request,
                        new ParameterizedTypeReference<List<RipleyOrderDTO>>() {}
                );

        return response.getBody();
    }
}