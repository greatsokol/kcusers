package org.gs.kcusers.repositories;

import org.gs.kcusers.domain.Login;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LoginRepository extends JpaRepository<Login, String> {
    Page<Login> findByUserNameOrderByAuthTimeDesc(String userName, Pageable pagable);
}

