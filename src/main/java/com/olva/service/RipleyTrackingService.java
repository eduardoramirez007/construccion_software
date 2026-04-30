package com.olva.service;

import com.olva.client.RipleyOrdersClient;
import com.olva.dto.RipleyOrderDTO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RipleyTrackingService {

    private final RipleyOrdersClient client;

    public RipleyTrackingService(RipleyOrdersClient client) {
        this.client = client;
    }

    public void processTrackingEvents() {

        List<RipleyOrderDTO> orders = client.getUnprocessedOrders();

        if (orders == null || orders.isEmpty()) {
            System.out.println("No hay órdenes nuevas");
            return;
        }

        for (RipleyOrderDTO order : orders) {

            System.out.println("Procesando orden: " + order.getOrderNumber());

            // 👉 Aquí haces:
            // 1. Guardar en BD
            // 2. Enviar evento a Serhafen
            // 3. Preparar trazabilidad
        }
    }
}