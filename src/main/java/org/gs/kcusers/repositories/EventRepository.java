package org.gs.kcusers.repositories;

import org.gs.kcusers.domain.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventRepository extends JpaRepository<Event, String> {
    Page<Event> findByUserNameAndRealmNameOrderByCreatedDesc(
            String userName,
            String realmName,
            Pageable pagable);
}
