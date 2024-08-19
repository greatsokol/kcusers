package org.gs.kcusers.repositories;

import org.gs.kcusers.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    User findByUserNameAndRealmName(String userName, String realmName);

    Page<User> findByUserNameContainingOrderByRealmNameAscUserNameAsc(String userName, Pageable pagable);

    Page<User> findAllByOrderByRealmNameAscUserNameAsc(Pageable pagable);

    long countByEnabled(boolean enabled);
}
