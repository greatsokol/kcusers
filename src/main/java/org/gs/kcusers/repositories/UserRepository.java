package org.gs.kcusers.repositories;

import org.gs.kcusers.domain.User;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    User findByUserNameAndRealmName(String userName, String realmName);

    Page<User> findByUserNameContainingOrderByRealmNameAscUserNameAsc(String userName, Pageable pagable);

    Page<User> findAllByOrderByRealmNameAscUserNameAsc(Pageable pagable);

    @Query(value = "SELECT DISTINCT realmName FROM User")
    List<String>  finaAllRealmNames();

    @Query(value = "SELECT DISTINCT realmName FROM User WHERE userName LIKE %:userName%")
    List<String>  finaAllRealmNamesContaining(@Param("userName") String userName);

    long countByEnabled(boolean enabled);
}
