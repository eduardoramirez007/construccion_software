package com.olva.academic.orm;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "MOCK_RIPLEY_OUTBOX")
public class MockRipleyOutboxEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "SHIPMENT_ID", nullable = false)
    private Long shipmentId;

    @Column(name = "EVENT_CODE", nullable = false, length = 50)
    private String eventCode;

    @Column(name = "STATUS", nullable = false, length = 20)
    private String status;

    @Column(name = "PAYLOAD", nullable = false, length = 4000)
    private String payload;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "outbox", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MockRipleyEventAuditEntity> audits = new ArrayList<>();

    protected MockRipleyOutboxEntity() {
    }

    public MockRipleyOutboxEntity(Long shipmentId, String eventCode, String status, String payload, LocalDateTime createdAt) {
        this.shipmentId = shipmentId;
        this.eventCode = eventCode;
        this.status = status;
        this.payload = payload;
        this.createdAt = createdAt;
    }

    public void addAudit(String auditStatus, String detail, LocalDateTime auditDate) {
        audits.add(new MockRipleyEventAuditEntity(this, auditStatus, detail, auditDate));
    }

    public Long getId() {
        return id;
    }

    public Long getShipmentId() {
        return shipmentId;
    }

    public String getEventCode() {
        return eventCode;
    }

    public String getStatus() {
        return status;
    }

    public String getPayload() {
        return payload;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public List<MockRipleyEventAuditEntity> getAudits() {
        return List.copyOf(audits);
    }
}
