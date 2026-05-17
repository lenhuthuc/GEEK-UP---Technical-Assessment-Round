package com.geekup.concert.concert;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface ConcertRepository extends JpaRepository<Concert, Long> {

    @Query("SELECT c FROM Concert c WHERE c.status = 'PUBLISHED' AND c.startsAt > :now ORDER BY c.startsAt ASC")
    Page<Concert> findUpcomingPublished(@Param("now") Instant now, Pageable pageable);

    @Query("SELECT c FROM Concert c WHERE c.status = 'PUBLISHED' ORDER BY c.startsAt ASC")
    Page<Concert> findAllPublished(Pageable pageable);
}
