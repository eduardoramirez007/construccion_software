package com.olva.academic.orm;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MockRipleyEventOrmServiceTest {

    private final MockRipleyOutboxJpaRepository repository = mock(MockRipleyOutboxJpaRepository.class);
    private final MockRipleyEventOrmService service = new MockRipleyEventOrmService(repository);

    @Test
    void shouldPersistCreatedEventUsingOrmRepository() {
        when(repository.save(any(MockRipleyOutboxEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MockRipleyOutboxEntity entity = service.registerCreatedEvent(100L, " rp-created ", "{\"id\":100}");

        assertThat(entity.getShipmentId()).isEqualTo(100L);
        assertThat(entity.getEventCode()).isEqualTo("RP-CREATED");
        assertThat(entity.getStatus()).isEqualTo("PENDING");
        assertThat(entity.getAudits()).hasSize(1);
        assertThat(entity.getAudits().getFirst().getStatus()).isEqualTo("CREATED");
        verify(repository).save(any(MockRipleyOutboxEntity.class));
    }

    @Test
    void shouldReadPendingEventsOrderedByCreationDate() {
        MockRipleyOutboxEntity entity = new MockRipleyOutboxEntity(100L, "RP-CREATED", "PENDING", "{}", java.time.LocalDateTime.now());
        when(repository.findByStatusOrderByCreatedAtAsc("PENDING")).thenReturn(List.of(entity));

        List<MockRipleyOutboxEntity> pending = service.findPendingEvents();

        assertThat(pending).containsExactly(entity);
        verify(repository).findByStatusOrderByCreatedAtAsc("PENDING");
    }

    @Test
    void shouldRejectInvalidPayloadBeforePersisting() {
        assertThatThrownBy(() -> service.registerCreatedEvent(100L, "RP-CREATED", " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("payload");
    }
}
