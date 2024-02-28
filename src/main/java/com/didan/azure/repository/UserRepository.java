package com.didan.azure.repository;

import com.didan.azure.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<Users, String> {
    Users findByUserId(String userId);
    Users findByUsername(String username);
}
