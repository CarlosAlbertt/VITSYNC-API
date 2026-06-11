package com.ejemplo.vitsync.repository;

import com.ejemplo.vitsync.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for append-only {@link AuditLog} records.
 *
 * @author VitSync Team
 * @version 1.0
 * @since 2.0
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * Audit trail of a given actor, paginated and newest-first via Pageable.
     *
     * @param actorNif NIF of the actor
     * @param pageable page request
     * @return page of audit records
     */
    Page<AuditLog> findByActorNif(String actorNif, Pageable pageable);

    /**
     * Re-points an actor's audit rows to a pseudonym (GDPR erasure): keeps the
     * legal trail but decouples it from the identified person.
     *
     * @param oldNif    current NIF
     * @param pseudonym replacement identifier
     * @return number of rows updated
     */
    @Modifying
    @Query("UPDATE AuditLog a SET a.actorNif = :pseudonym WHERE a.actorNif = :oldNif")
    int pseudonymizeActor(@Param("oldNif") String oldNif, @Param("pseudonym") String pseudonym);
}
