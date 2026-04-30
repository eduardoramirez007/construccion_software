package com.olva.academic.orm;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MockRipleyOutboxJpaRepository extends JpaRepository<MockRipleyOutboxEntity, Long> {

    List<MockRipleyOutboxEntity> findByStatusOrderByCreatedAtAsc(String status);
}
