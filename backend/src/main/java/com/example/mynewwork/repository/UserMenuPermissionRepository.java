package com.example.mynewwork.repository;

import com.example.mynewwork.model.entity.UserMenuPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserMenuPermissionRepository extends JpaRepository<UserMenuPermission, Long> {
    Optional<UserMenuPermission> findByUserId(Long userId);
    void deleteByUserId(Long userId);
}
