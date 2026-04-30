package com.olva.academic.orm;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "MOCK_RIPLEY_EVENT_AUDIT")
public class MockRipleyEventAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "OUTBOX_ID", nullable = false)
    private MockRipleyOutboxEntity outbox;

    @Column(name = "STATUS", nullable = false, length = 20)
    private String status;

    @Column(name = "DETAIL", length = 500)
    private String detail;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    protected MockRipleyEventAuditEntity() {
    }

    public MockRipleyEventAuditEntity(MockRipleyOutboxEntity outbox, String status, String detail, LocalDateTime createdAt) {
        this.outbox = outbox;
        this.status = status;
        this.detail = detail;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public MockRipleyOutboxEntity getOutbox() {
        return outbox;
    }

    public String getStatus() {
        return status;
    }

    public String getDetail() {
        return detail;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
