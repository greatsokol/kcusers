package org.gs.kcusers.repositories;

import org.gs.kcusers.domain.Audit;
import org.springframework.data.jpa.repository.JpaRepository;


public interface AuditRepository extends JpaRepository<Audit, String> {
}
