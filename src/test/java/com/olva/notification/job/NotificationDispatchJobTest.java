package com.olva.notification.job;

import com.olva.notification.service.NotificationDispatchService;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class NotificationDispatchJobTest {

    @Test
    void shouldDispatchAllNotificationsWhenJobRuns() {
        NotificationDispatchService dispatchService = mock(NotificationDispatchService.class);
        NotificationDispatchJob job = new NotificationDispatchJob(dispatchService);

        job.executeInternal(null);

        verify(dispatchService, times(1)).dispatchAll();
    }
}
