package com.olva.notification.repository;

import com.olva.notification.model.NotificationAttemptRecord;

public interface NotificationAttemptRepository {

    void save(NotificationAttemptRecord attemptRecord);
}
