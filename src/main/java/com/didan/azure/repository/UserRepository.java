package com.didan.azure.repository;

import com.didan.azure.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<Users, String> {
    Users findFirstByUsername(String username);
    Users findFirstByUserId(String userId);
}
