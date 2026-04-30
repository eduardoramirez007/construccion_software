package com.olva.academic.orm;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MockRipleyEventOrmService {

    private final MockRipleyOutboxJpaRepository repository;

    public MockRipleyEventOrmService(MockRipleyOutboxJpaRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public MockRipleyOutboxEntity registerCreatedEvent(Long shipmentId, String eventCode, String payload) {
        validate(shipmentId, eventCode, payload);
        LocalDateTime now = LocalDateTime.now();
        MockRipleyOutboxEntity outbox = new MockRipleyOutboxEntity(
                shipmentId,
                eventCode.trim().toUpperCase(),
                "PENDING",
                payload,
                now
        );
        outbox.addAudit("CREATED", "Evento mock registrado mediante ORM", now);
        return repository.save(outbox);
    }

    @Transactional(readOnly = true)
    public List<MockRipleyOutboxEntity> findPendingEvents() {
        return repository.findByStatusOrderByCreatedAtAsc("PENDING");
    }

    private void validate(Long shipmentId, String eventCode, String payload) {
        if (shipmentId == null || shipmentId <= 0) {
            throw new IllegalArgumentException("El envio debe ser positivo");
        }
        if (eventCode == null || eventCode.isBlank()) {
            throw new IllegalArgumentException("El codigo de evento es obligatorio");
        }
        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException("El payload es obligatorio");
        }
    }
}
