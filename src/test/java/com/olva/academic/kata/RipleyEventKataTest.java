package com.olva.academic.kata;

import com.olva.notification.model.NotificationBatchItem;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RipleyEventKataTest {

    private final RipleyEventKata kata = new RipleyEventKata();

    @Test
    void shouldNormalizeEventCodeAndRemoveDuplicatedCuds() {
        NotificationBatchItem item = buildItem(" rp-created ", List.of(" cud-1 ", "CUD-1", "", "cud-2"));

        RipleyEventSummary summary = kata.summarize(item);

        assertThat(summary.notificationId()).isEqualTo(1L);
        assertThat(summary.shipmentId()).isEqualTo(100L);
        assertThat(summary.eventCode()).isEqualTo("rp-created");
        assertThat(summary.normalizedCuds()).containsExactly("CUD-1", "CUD-2");
    }

    @Test
    void shouldRejectEventsWithoutRipleyEventCode() {
        NotificationBatchItem item = buildItem(" ", List.of("CUD-1"));

        assertThatThrownBy(() -> kata.summarize(item))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("codigo de evento");
    }

    private NotificationBatchItem buildItem(String eventCode, List<String> cuds) {
        LocalDateTime now = LocalDateTime.of(2026, 4, 29, 22, 0);
        return new NotificationBatchItem(
                1L, "RIPLEY", 100L, "ORDER-1", "TRACK-1", "CARRIER-1",
                "Olva", "OLVA", null, null, 200L, 9140L, 300L, eventCode,
                "Creado", 400L, "01", 12345L, "Registrado", 0,
                now, now, now, now, cuds, List.of()
        );
    }
}
